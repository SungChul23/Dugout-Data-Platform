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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBedrockService {

    private final BedrockRuntimeClient bedrockClient;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    private static final String FEW_SHOT_EXAMPLES = """
            [Few-shot 예시 - 반드시 아래 패턴처럼 SQL만 반환할 것]
            
            Q: 홈런 1위 타자 알려줘
            A: SELECT p.name, h.h_hr, t.name AS team_name
               FROM daily_player_hitter h
               JOIN player p ON h.player_id = p.player_id
               JOIN team t ON h.team_id = t.id
               WHERE h.base_date = (SELECT MAX(base_date) FROM daily_player_hitter)
               AND h.h_pa >= (SELECT MAX(h_g) FROM daily_player_hitter) * 3.1
               ORDER BY h.h_hr DESC LIMIT 1;
            
            Q: SSG 투수 중 ERA 가장 낮은 선수
            A: SELECT p.name, pi.p_era, t.name AS team_name
               FROM daily_player_pitcher pi
               JOIN player p ON pi.player_id = p.player_id
               JOIN team t ON pi.team_id = t.id
               WHERE pi.base_date = (SELECT MAX(base_date) FROM daily_player_pitcher)
               AND t.name LIKE '%SSG%'
               AND pi.p_ip >= (SELECT MAX(p_g) FROM daily_player_pitcher) * 1.0
               ORDER BY pi.p_era ASC LIMIT 1;
            
            Q: SSG 경기 일정 알려줘
            A: SELECT g.game_date, g.game_time,
                      ht.name AS home_team, at.name AS away_team,
                      g.stadium_name, g.status
               FROM game g
               JOIN team ht ON g.home_team_id = ht.id
               JOIN team at ON g.away_team_id = at.id
               WHERE (ht.name LIKE '%SSG%' OR at.name LIKE '%SSG%')
               AND g.game_date >= CURDATE()
               ORDER BY g.game_date ASC, g.game_time ASC
               LIMIT 10;
            
            Q: 어제 경기 결과 알려줘
            A: SELECT ht.name AS home_team, g.home_score,
                      at.name AS away_team, g.away_score,
                      g.status, g.stadium_name
               FROM game g
               JOIN team ht ON g.home_team_id = ht.id
               JOIN team at ON g.away_team_id = at.id
               WHERE g.game_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
               AND g.status = 'FINISHED'
               ORDER BY g.game_time ASC;
            
            Q: 현재 팀 순위 알려줘
            A: SELECT t.name AS team_name, dr.team_rank,
                      dr.wins, dr.losses, dr.draws, dr.win_rate, dr.streak
               FROM daily_team_ranking dr
               JOIN team t ON dr.team_id = t.id
               WHERE dr.ranking_date = (SELECT MAX(ranking_date) FROM daily_team_ranking)
               ORDER BY dr.team_rank ASC;
            
            Q: 연승 중인 팀 있어?
            A: SELECT t.name AS team_name, dr.streak, dr.team_rank
               FROM daily_team_ranking dr
               JOIN team t ON dr.team_id = t.id
               WHERE dr.ranking_date = (SELECT MAX(ranking_date) FROM daily_team_ranking)
               AND dr.streak LIKE '%승%'
               ORDER BY CAST(REPLACE(dr.streak, '승', '') AS SIGNED) DESC;
            
            Q: 연패 중인 팀 어디야?
            A: SELECT t.name AS team_name, dr.streak, dr.team_rank
               FROM daily_team_ranking dr
               JOIN team t ON dr.team_id = t.id
               WHERE dr.ranking_date = (SELECT MAX(ranking_date) FROM daily_team_ranking)
               AND dr.streak LIKE '%패%'
               ORDER BY CAST(REPLACE(dr.streak, '패', '') AS SIGNED) DESC;
            
            Q: FA A등급 선수 알려줘
            A: SELECT player_name, grade, age, sub_position_type,
                      stat_contribution, fa_status, current_salary
               FROM fa_market
               WHERE grade = 'A'
               AND is_fa_target = 1
               AND fa_status != '잔류'
               ORDER BY stat_contribution DESC;
            
            Q: 박성한 알아?
            A: SELECT p.name, p.position_type, p.sub_position_type,
                      p.back_number, t.name AS team_name
               FROM player p
               JOIN team t ON p.team_id = t.id
               WHERE p.name = '박성한';
            
            Q: 김도영 어떤 선수야?
            A: SELECT p.name, p.position_type, p.back_number,
                      t.name AS team_name,
                      h.h_avg, h.h_hr, h.h_ops
               FROM player p
               JOIN team t ON p.team_id = t.id
               LEFT JOIN daily_player_hitter h ON h.player_id = p.player_id
               AND h.base_date = (SELECT MAX(base_date) FROM daily_player_hitter)
               WHERE p.name = '김도영';
            """;

    /**
     * 1차 Bedrock 호출 - SQL 생성
     */
    public String generateSql(String schema, String conversationHistory, String question) {
        log.info("[ChatBedrockService] SQL 생성 요청: {}", question);

        String prompt = String.format("""
                너는 KBO 야구 데이터베이스 전문가야. 아래 스키마를 보고 MySQL SQL을 생성해줘.
                
                [절대 규칙 - 반드시 지킬 것]
                1. SELECT 쿼리만 생성 (INSERT/UPDATE/DELETE/DROP 절대 금지)
                2. SQL 코드만 반환 (이모지, 설명, 주석, 결과 예시 모두 금지)
                3. SQL 앞뒤에 어떤 텍스트도 절대 포함하지 말 것
                4. 마크다운 코드블록(```) 사용 금지
                5. 날짜 조건 미명시 시 MAX(base_date) 또는 MAX(ranking_date) 사용
                6. daily_team_ranking 조회 시 ranking_date 사용 (base_date 금지)
                7. 팀명 검색 시 LIKE '%%팀키워드%%' 사용
                8. 경기 일정 조회 시 game_date >= CURDATE(), ORDER BY ASC
                9. 경기 결과 조회 시 status = 'FINISHED' 조건 추가
                10. FA 조회 시 fa_status != '잔류' 조건 반드시 포함
                11. 선수 정보 질문(~알아? ~누구야? ~어떤 선수?)도
                    반드시 player/daily_player_hitter/pitcher 테이블을 SQL로 조회
                    절대 학습 데이터로 직접 답변 금지
                
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

    /**
     * 2차 Bedrock 호출 - 자연어 답변 생성
     */
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
                4. 항목이 많으면 줄바꿈으로 구분하여 가독성 있게 작성
                5. 이모지 적절히 사용 (⚾ 등)
                6. "더그아웃 AI 데이터베이스" 같은 불필요한 문구 절대 금지
                7. "자세한 정보는 ~에서 확인하세요" 같은 안내 문구 금지
                8. FA 등급 관련 답변 시 마지막에 반드시 추가:
                   "※ 더그아웃이 경기력 데이터로 분석한 예측 등급입니다. 상세 분석은 [FA 시장 등급 분석] 메뉴를 이용해주세요."
                9. 골든글러브 관련 답변 시 마지막에 반드시 추가:
                   "※ 실제 수상이 아닌 더그아웃 AI의 예측 결과입니다. 상세 분석은 [골든글러브 수상 예측] 메뉴를 이용해주세요."
                10. 경기 일정 답변 시 날짜 / 시간 / 상대팀 / 구장 순서로 정리
                
                [사용자 질문]
                %s
                
                [DB 조회 결과]
                %s
                
                답변:
                """,
                question,
                resultStr
        );

        return invoke(prompt, 600, 0.7);
    }

    /**
     * 야구 도메인 질문인지 판별
     */
    public boolean isBaseballQuestion(String question) {
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