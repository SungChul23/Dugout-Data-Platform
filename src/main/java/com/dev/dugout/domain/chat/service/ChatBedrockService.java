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
    private final PromptLoader promptLoader;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    /**
     * 1차 Bedrock 호출 - SQL 생성
     */
    public String generateSql(String schema, String conversationHistory, String question) {
        log.info("[ChatBedrockService] SQL 생성 요청: {}", question);

        String systemPrompt = promptLoader.get(PromptLoader.SQL_SYSTEM);
        String fewShot      = promptLoader.get(PromptLoader.SQL_FEWSHOT);

        String prompt = String.format(
                "%s\n\n[DB 스키마]\n%s\n\n%s\n\n[대화 히스토리]\n%s\n\n[현재 질문]\n%s\n\nSQL:",
                systemPrompt,
                schema,
                fewShot,
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

        String systemPrompt = promptLoader.get(PromptLoader.ANS_SYSTEM);

        String resultStr = dbResult.isEmpty()
                ? "조회 결과가 없습니다."
                : dbResult.toString();

        String prompt = String.format(
                "%s\n\n[사용자 질문]\n%s\n\n[DB 조회 결과]\n%s\n\n답변:",
                systemPrompt,
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