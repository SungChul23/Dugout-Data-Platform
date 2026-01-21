package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.infrastructure.aws.dto.SurveyRequestDto;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
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
@Slf4j // 로깅을 위한 어노테이션
public class RecommendService {

    private final AthenaClient athenaClient;
    private final RecommendedBedrockService bedrockService;

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

    public TeamRecommendationResponseDto getMatchTeam(SurveyRequestDto request) {
        // [LOG] 분석 시작 및 수신 데이터 확인
        log.info(">>>> [ANALYSIS START] KBO 팀 추천 엔진 가동 (기준 연도: {} 이후)", request.getStartYear());
        log.info(">>>> [USER PREFERENCE] {}", request.getPreferenceSummary());

        Map<String, Integer> prefs = request.getPreferences();
        double w1 = prefs.getOrDefault("q1", 3) / 5.0;
        double w2 = prefs.getOrDefault("q2", 3) / 5.0;
        double w3 = prefs.getOrDefault("q3", 3) / 5.0;
        double w4 = prefs.getOrDefault("q4", 3) / 5.0;
        double w5 = prefs.getOrDefault("q5", 3) / 5.0;
        double w6 = prefs.getOrDefault("q6", 3) / 5.0;

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

        // [LOG] 생성된 SQL 확인 (디버깅 시 가장 중요함)
        log.debug(">>>> [SQL GENERATED] {}", sql);

        return executeAthenaQuery(sql, request.getPreferenceSummary());
    }

    private TeamRecommendationResponseDto executeAthenaQuery(String sql, String userPref) {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        String executionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();

        // [LOG] 쿼리 실행 ID 기록
        log.info(">>>> [ATHENA SUBMITTED] Query ID: {}", executionId);

        waitForQuery(executionId);

        GetQueryResultsResponse results = athenaClient.getQueryResults(
                GetQueryResultsRequest.builder().queryExecutionId(executionId).maxResults(2).build()
        );

        List<Row> rows = results.resultSet().rows();
        if (rows.size() > 1) {
            List<Datum> data = rows.get(1).data();

            String year = data.get(0).varCharValue();
            String dbTeamName = data.get(1).varCharValue();
            String hr = data.get(2).varCharValue();
            String avg = data.get(3).varCharValue();
            String era = data.get(4).varCharValue();
            String ops = data.get(5).varCharValue();
            double totalScore = Double.parseDouble(data.get(6).varCharValue());

            String fullTeamName = FULL_TEAM_NAMES.getOrDefault(dbTeamName, dbTeamName + " 구단");
            String statsSummary = String.format("홈런 %s개, 타율 %s, ERA %s, OPS %s", hr, avg, era, ops);

            // [LOG] 아테나 결과 확인
            log.info(">>>> [ATHENA RESULT] 매칭 성공! -> {}년 {} (Score: {})", year, fullTeamName, totalScore);

            // [LOG] Bedrock 호출 시작
            log.info(">>>> [BEDROCK REQUEST] AI 추천 사유 생성 시작...");
            String aiReason = bedrockService.generateReason(fullTeamName, year, statsSummary, userPref);
            log.info(">>>> [BEDROCK RESPONSE] 생성 완료");

            return TeamRecommendationResponseDto.builder()
                    .year(year)
                    .originalName(dbTeamName)
                    .teamName(fullTeamName)
                    .score(totalScore)
                    .reason(aiReason)
                    .build();
        }

        log.warn(">>>> [NO DATA] 조건에 맞는 팀을 찾을 수 없습니다.");
        throw new RuntimeException("추천 팀 데이터가 없습니다.");
    }

    private void waitForQuery(String id) {
        int attempts = 0;
        while (true) {
            GetQueryExecutionResponse res = athenaClient.getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(id).build());
            String state = res.queryExecution().status().state().toString();

            if (state.equals("SUCCEEDED")) {
                log.info(">>>> [ATHENA SUCCESS] 쿼리 완료 ({}회 폴링)", attempts);
                return;
            }
            if (state.equals("FAILED") || state.equals("CANCELLED")) {
                String reason = res.queryExecution().status().stateChangeReason();
                log.error(">>>> [ATHENA ERROR] 쿼리 실패 (ID: {}), 사유: {}", id, reason);
                throw new RuntimeException("Athena 실패: " + reason);
            }

            try {
                attempts++;
                Thread.sleep(500);
            } catch (Exception ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}