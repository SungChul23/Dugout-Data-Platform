package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.infrastructure.aws.dto.SurveyRequest;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final AthenaClient athenaClient;
    private final BedrockService bedrockService; // [추가] AI 서비스 주입

    @Value("${aws.athena.database}")
    private String database;
    @Value("${aws.athena.output-location}")
    private String outputLocation;

    private static final Map<String, String> FULL_TEAM_NAMES = Map.ofEntries(
            Map.entry("1", "삼성 라이온즈"), Map.entry("2", "두산 베어스"),
            Map.entry("3", "LG 트윈스"), Map.entry("4", "롯데 자이언츠"),
            Map.entry("5", "KIA 타이거즈"), Map.entry("6", "한화 이글스"),
            Map.entry("7", "SSG 랜더스"), Map.entry("8", "키움 히어로즈"),
            Map.entry("9", "NC 다이노스"), Map.entry("10", "kt wiz"),
            Map.entry("11", "현대 유니콘스")
    );

    public TeamRecommendationResponse getMatchTeam(SurveyRequest request) {
        log.info("==> KBO 추천 분석 시작 (연도: {}, 가중치 계산 중)", request.getStartYear());

        // 1. 가중치 계산 (0.0 ~ 1.0)
        Map<String, Integer> prefs = request.getPreferences();
        double w1 = prefs.getOrDefault("q1", 3) / 5.0; // 홈런
        double w2 = prefs.getOrDefault("q2", 3) / 5.0; // 타율
        double w3 = prefs.getOrDefault("q3", 3) / 5.0; // 방어율
        double w4 = prefs.getOrDefault("q4", 3) / 5.0; // 세이브/홀드
        double w5 = prefs.getOrDefault("q5", 3) / 5.0; // OPS
        double w6 = prefs.getOrDefault("q6", 3) / 5.0; // 승률

        // 2. 아테나 SQL 작성 (AI에게 넘겨줄 원본 지표 hr, avg, era, ops 포함)
        String sql = String.format(
                "SELECT h.year, h.\"팀명\", " +
                        "CAST(h.hr AS DOUBLE), CAST(h.avg AS DOUBLE), CAST(p.era AS DOUBLE), CAST(h.ops AS DOUBLE), " +
                        "( (CAST(h.hr AS DOUBLE) / 234.0 * %.4f) + " +
                        "(CAST(h.avg AS DOUBLE) / 0.300 * %.4f) + " +
                        "((5.0 - CAST(p.era AS DOUBLE)) / 5.0 * %.4f) + " +
                        "((CAST(p.sv AS DOUBLE) + CAST(p.hld AS DOUBLE)) / 140.0 * %.4f) + " +
                        "(CAST(h.ops AS DOUBLE) / 1.0 * %.4f) + " +
                        "(CAST(p.wpct AS DOUBLE) / 1.0 * %.4f) ) AS total_score " +
                        "FROM type_hitter h JOIN type_pitcher p ON h.year = p.year AND h.team_id = p.team_id " +
                        "WHERE h.year >= '%d' " +
                        "ORDER BY total_score DESC LIMIT 1",
                w1, w2, w3, w4, w5, w6, request.getStartYear()
        );

        // 3. 쿼리 실행 및 결과 처리 (Bedrock 호출 포함) + getPreferenceSummary 통해 자세한 정보를 베드락에게 전달
        return executeAthenaQuery(sql, request.getPreferenceSummary());
    }

    private TeamRecommendationResponse executeAthenaQuery(String sql, String userPref) {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        String executionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();
        waitForQuery(executionId);

        GetQueryResultsResponse results = athenaClient.getQueryResults(
                GetQueryResultsRequest.builder().queryExecutionId(executionId).maxResults(2).build()
        );

        List<Row> rows = results.resultSet().rows();
        if (rows.size() > 1) {
            List<Datum> data = rows.get(1).data();

            // 데이터 매핑 (SQL SELECT 순서와 일치해야 함)
            String year = data.get(0).varCharValue();
            String dbTeamName = data.get(1).varCharValue();
            String hr = data.get(2).varCharValue();
            String avg = data.get(3).varCharValue();
            String era = data.get(4).varCharValue();
            String ops = data.get(5).varCharValue();
            double totalScore = Double.parseDouble(data.get(6).varCharValue());

            String fullTeamName = FULL_TEAM_NAMES.getOrDefault(dbTeamName, dbTeamName + " 구단");
            String statsSummary = String.format("홈런 %s개, 타율 %s, 평균자책점 %s, OPS %s", hr, avg, era, ops);

            // [핵심] Bedrock AI에게 해설 요청
            String aiReason = bedrockService.generateReason(fullTeamName, year, statsSummary, userPref);

            return TeamRecommendationResponse.builder()
                    .year(year)
                    .originalName(dbTeamName)
                    .teamName(fullTeamName)
                    .score(totalScore)
                    .reason(aiReason) // AI가 쓴 문장이 담깁니다.
                    .build();
        }
        throw new RuntimeException("추천 팀 데이터가 없습니다.");
    }

    private void waitForQuery(String id) {
        while (true) {
            GetQueryExecutionResponse res = athenaClient.getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(id).build());
            String state = res.queryExecution().status().state().toString();
            if (state.equals("SUCCEEDED")) return;
            if (state.equals("FAILED") || state.equals("CANCELLED")) throw new RuntimeException("Athena 실패: " + id);
            try { Thread.sleep(500); } catch (Exception ignored) {}
        }
    }
}