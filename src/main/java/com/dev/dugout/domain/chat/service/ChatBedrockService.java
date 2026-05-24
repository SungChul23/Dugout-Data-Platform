package com.dev.dugout.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

//역할: Bedrock 호출 담당 (2가지)

//generateSql()      → SQL 생성 (1차 호출)
//generateAnswer()   → 자연어 변환 (2차 호출)
//isBaseballQuestion() → 야구 도메인 판별

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBedrockService {

    private final BedrockRuntimeClient bedrockClient;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    // Few-shot 예시 SQL (SQL 생성 품질 향상)
    private static final String FEW_SHOT_EXAMPLES = """
            [Few-shot 예시]
            Q: 홈런 1위 타자 알려줘
            A: SELECT p.name, h.h_hr, t.name AS team_name
               FROM daily_player_hitter h
               JOIN player p ON h.player_id = p.player_id
               JOIN team t ON h.team_id = t.id
               WHERE h.base_date = (SELECT MAX(base_date) FROM daily_player_hitter)
               AND h.h_pa >= (SELECT MAX(h_g) FROM daily_player_hitter) * 3.1
               ORDER BY h.h_hr DESC LIMIT 1
            
            Q: SSG 투수 중 ERA 가장 낮은 선수
            A: SELECT p.name, pi.p_era, t.name AS team_name
               FROM daily_player_pitcher pi
               JOIN player p ON pi.player_id = p.player_id
               JOIN team t ON pi.team_id = t.id
               WHERE pi.base_date = (SELECT MAX(base_date) FROM daily_player_pitcher)
               AND t.name LIKE '%SSG%'
               AND pi.p_ip >= (SELECT MAX(p_g) FROM daily_player_pitcher) * 1.0
               ORDER BY pi.p_era ASC LIMIT 1
            
            Q: 오늘 경기 결과 알려줘
            A: SELECT ht.name AS home_team, g.home_score,
                      at.name AS away_team, g.away_score,
                      g.status, g.stadium_name
               FROM game g
               JOIN team ht ON g.home_team_id = ht.id
               JOIN team at ON g.away_team_id = at.id
               WHERE g.game_date = CURDATE()
               ORDER BY g.game_time
            
            Q: 현재 팀 순위 알려줘
            A: SELECT t.name AS team_name, dr.team_rank,
                      dr.wins, dr.losses, dr.draws, dr.win_rate, dr.streak
               FROM daily_team_ranking dr
               JOIN team t ON dr.team_id = t.id
               WHERE dr.ranking_date = (SELECT MAX(ranking_date) FROM daily_team_ranking)
               ORDER BY dr.team_rank ASC
            
            Q: FA A등급 선수 알려줘
            A: SELECT player_name, grade, age, sub_position_type,
                      stat_contribution, fa_status, current_salary
               FROM fa_market
               WHERE grade = 'A' AND is_fa_target = 1
               ORDER BY stat_contribution DESC
            """;

    // 💡 1차 Bedrock 호출 - SQL 생성
    public String generateSql(String schema, String conversationHistory, String question) {
        log.info("[ChatBedrockService] SQL 생성 요청: {}", question);

        String prompt = String.format("""
                너는 KBO 야구 데이터베이스 전문가야. 아래 스키마를 보고 사용자 질문에 맞는 MySQL SQL을 생성해줘.
                
                [중요 규칙]
                1. SELECT 쿼리만 생성 (INSERT/UPDATE/DELETE/DROP 절대 금지)
                2. 날짜 조건 미명시 시 반드시 MAX(base_date) 또는 MAX(ranking_date) 사용
                3. daily_team_ranking은 ranking_date 사용 (base_date 사용 금지!)
                4. 팀명 검색 시 LIKE '%%팀키워드%%' 사용
                5. SQL만 반환 (설명 없이 순수 SQL만)
                6. 마크다운 코드블록 없이 SQL만 반환
                
                [DB 스키마]
                %s
                
                %s
                
                [대화 히스토리]
                %s
                
                [현재 질문]
                %s
                
                SQL:
                """,
                schema,
                FEW_SHOT_EXAMPLES,
                conversationHistory,
                question
        );

        return invoke(prompt, 500, 0.0);
    }

    // 💡 2차 Bedrock 호출 - 자연어 답변 생성
    public String generateAnswer(String question, List<Map<String, Object>> dbResult) {
        log.info("[ChatBedrockService] 자연어 변환 요청");

        String resultStr = dbResult.isEmpty()
                ? "조회 결과가 없습니다."
                : dbResult.toString();

        String prompt = String.format("""
                너는 KBO 야구 데이터 전문 AI 어시스턴트 '더그아웃 AI'야.
                아래 DB 조회 결과를 바탕으로 사용자 질문에 자연스러운 한국어로 답변해줘.
                
                [규칙]
                1. 친절하고 자연스러운 한국어로 답변
                2. 데이터가 없으면 "해당 데이터를 찾을 수 없습니다" 안내
                3. 야구 전문용어는 그대로 사용 (ERA, OPS, WHIP 등)
                4. 답변은 3~5문장 이내로 간결하게
                
                [사용자 질문]
                %s
                
                [DB 조회 결과]
                %s
                
                답변:
                """,
                question,
                resultStr
        );

        return invoke(prompt, 500, 0.7);
    }

    // 야구 도메인 질문인가 ?
    // 아쉬운 부분, LLM한테 판별 위임해야하나 ?
    // -> 베드락 추가 호출이 진행 (비용+속도 증가)

    public boolean isBaseballQuestion(String question) {
        // 명백히 야구와 무관한 키워드
        List<String> nonBaseballKeywords = List.of(
                "주식", "코인", "부동산", "날씨", "맛집", "레시피",
                "쇼핑", "영화", "드라마", "음악", "정치", "선거",
                "의학", "병원", "법률", "세금"
        );

        for (String keyword : nonBaseballKeywords) {
            if (question.contains(keyword)) {
                log.info("[ChatBedrockService] 비야구 질문 감지: {}", keyword);
                return false;
            }
        }
        return true;
    }

    //Bedrock 공통 호출
    private String invoke(String prompt, int maxTokens, double temperature) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("anthropic_version", "bedrock-2023-05-31");
            payload.put("max_tokens", maxTokens);
            payload.put("temperature", temperature);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt));
            payload.put("messages", messages);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            return new JSONObject(response.body().asUtf8String())
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

        } catch (Exception e) {
            log.error("[ChatBedrockService] Bedrock 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 응답 생성 중 오류가 발생했습니다.");
        }
    }
}