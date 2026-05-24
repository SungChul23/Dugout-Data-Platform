package com.dev.dugout.domain.chat.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
// 역할: 2차 LLM 테이블 선택
// SchemaRouter 실패 시에만 호출
// ex) 요즘 방망이가 뜨거운 ..

public class LlmSchemaRouter {

    private final BedrockRuntimeClient bedrockClient;
    private final SchemaContextBuilder schemaContextBuilder;

    // 테이블 번호 → 테이블명 매핑
    private static final List<String> TABLE_INDEX = List.of(
            "daily_player_hitter",
            "daily_player_pitcher",
            "daily_team_ranking",
            "daily_team_stats",
            "game",
            "prediction_result",
            "fa_market",
            "gg_leaderboard",
            "player",
            "team"
    );


     //LLM에게 질문에 필요한 테이블 선택 위임
     //테이블 목록 + 한 줄 설명만 전달 (컬럼 정보 X → 토큰 절약)
    public List<String> route(String question) {
        log.info("[LlmSchemaRouter] LLM 라우팅 시작: {}", question);

        String tableList = schemaContextBuilder.buildTableListForRouting();

        String prompt = String.format("""
                %s
                
                질문: "%s"
                
                위 질문에 답하기 위해 필요한 테이블 번호만 JSON 배열로 반환해줘.
                반드시 아래 형식만 반환하고 다른 설명은 절대 쓰지 마.
                예시: {"tables": [1, 9, 10]}
                """, tableList, question);

        try {
            JSONObject payload = new JSONObject();
            payload.put("anthropic_version", "bedrock-2023-05-31");
            payload.put("max_tokens", 100);
            payload.put("temperature", 0.0);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt));
            payload.put("messages", messages);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseText = new JSONObject(response.body().asUtf8String())
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            List<String> selectedTables = parseTableNames(responseText);
            log.info("[LlmSchemaRouter] LLM 선택 테이블: {}", selectedTables);
            return selectedTables;

        } catch (Exception e) {
            log.error("[LlmSchemaRouter] LLM 라우팅 실패: {}", e.getMessage());
            // 실패 시 기본 테이블 반환
            return List.of("daily_player_hitter", "daily_player_pitcher",
                    "daily_team_ranking", "player", "team");
        }
    }

    //LLM 응답에서 테이블 번호 파싱 → 테이블명 변환
    private List<String> parseTableNames(String responseText) {
        List<String> result = new ArrayList<>();
        try {
            // JSON 파싱
            String cleaned = responseText
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JSONObject json = new JSONObject(cleaned);
            JSONArray tables = json.getJSONArray("tables");

            for (int i = 0; i < tables.length(); i++) {
                int tableNum = tables.getInt(i) - 1; // 1-based → 0-based
                if (tableNum >= 0 && tableNum < TABLE_INDEX.size()) {
                    result.add(TABLE_INDEX.get(tableNum));
                }
            }

            // player, team은 기본 추가
            if (!result.contains("player")) result.add("player");
            if (!result.contains("team")) result.add("team");

        } catch (Exception e) {
            log.error("[LlmSchemaRouter] 파싱 실패: {}", e.getMessage());
            return List.of("daily_player_hitter", "player", "team");
        }
        return result;
    }
}
