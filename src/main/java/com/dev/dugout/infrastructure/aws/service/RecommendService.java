package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.infrastructure.aws.dto.SurveyRequestDto;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.ArrayList;
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

    public List<TeamRecommendationResponseDto> getMatchTeam(SurveyRequestDto request) {
        log.info(">>>> [ANALYSIS START] KBO 상위 3팀 추천 엔진 가동");

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
                        "CAST(p.sv AS DOUBLE), CAST(p.hld AS DOUBLE), " +
                        "( (CAST(h.hr AS DOUBLE) / 234.0 * %.4f) + " +
                        "(CAST(h.avg AS DOUBLE) / 0.300 * %.4f) + " +
                        "((5.0 - CAST(p.era AS DOUBLE)) / 5.0 * %.4f) + " +
                        "((CAST(p.sv AS DOUBLE) + CAST(p.hld AS DOUBLE)) / 140.0 * %.4f) + " +
                        "(CAST(h.ops AS DOUBLE) / 1.0 * %.4f) + " +
                        "(CAST(p.wpct AS DOUBLE) / 1.0 * %.4f) ) AS total_score " +
                        "FROM type_hitter h JOIN type_pitcher p ON h.year = p.year AND h.team_id = p.team_id " +
                        "WHERE h.year >= '%d' " +
                        "ORDER BY total_score DESC LIMIT 3", // 3개 추출
                w1, w2, w3, w4, w5, w6, request.getStartYear()
        );

        return executeAthenaQuery(sql, request.getPreferenceSummary());
    }

    private List<TeamRecommendationResponseDto> executeAthenaQuery(String sql, String userPref) {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        String executionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();
        waitForQuery(executionId);

        // [수정] maxResults를 4로 설정 (헤더 1개 + 데이터 3개)
        GetQueryResultsResponse results = athenaClient.getQueryResults(
                GetQueryResultsRequest.builder().queryExecutionId(executionId).maxResults(4).build()
        );

        List<Row> rows = results.resultSet().rows();
        List<TeamRecommendationResponseDto> recommendations = new ArrayList<>();

        if (rows.size() > 1) {
            // [수정] index 1부터 전체 로우 반복 처리
            for (int i = 1; i < rows.size(); i++) {
                List<Datum> data = rows.get(i).data();

                String year = data.get(0).varCharValue();
                String dbTeamName = data.get(1).varCharValue();
                String fullTeamName = FULL_TEAM_NAMES.getOrDefault(dbTeamName, dbTeamName + " 구단");
                double totalScore = Double.parseDouble(data.get(8).varCharValue());

                // AI 분석을 위한 스탯 요약 (DTO에 statsSummary 필드가 있다고 가정)
                String stats = String.format("홈런 %s, 타율 %s, ERA %s, OPS %s, 세이브 %s, 홀드 %s",
                        data.get(2).varCharValue(), data.get(3).varCharValue(), data.get(4).varCharValue(),
                        data.get(5).varCharValue(), data.get(6).varCharValue(), data.get(7).varCharValue());

                recommendations.add(TeamRecommendationResponseDto.builder()
                        .year(year)
                        .originalName(dbTeamName)
                        .teamName(fullTeamName)
                        .score(totalScore)
                        .statsSummary(stats) // AI가 읽을 용도
                        .build());
            }

            // [핵심] 반복문 종료 후 Bedrock 단 1회 호출
            log.info(">>>> [BEDROCK REQUEST] 3개 팀 통합 분석 시작...");
            String totalAiReason = bedrockService.generateBatchReason(recommendations, userPref);

            // 모든 DTO에 생성된 리포트를 할당 (프론트에서 하나만 꺼내 쓰면 됨)
            recommendations.forEach(dto -> dto.setReason(totalAiReason));

            return recommendations;
        }

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