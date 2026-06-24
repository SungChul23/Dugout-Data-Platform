package com.dev.dugout.infrastructure.aws.bedrock;

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

/**
 * Bedrock API 호출을 통합 관리하는 Facade.
 * 4개 서비스(ChatBedrock, FaMarketBedrock, ReportBedrock, RecommendedBedrock)의
 * 중복된 payload 구성 / 요청 빌드 / 응답 파싱 로직을 캡슐화한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BedrockClientFacade {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    /**
     * Bedrock API 통합 호출 메서드.
     *
     * @param modelId       모델 식별자 (예: "anthropic.claude-3-haiku-20240307-v1:0")
     * @param maxTokens     최대 토큰 수 (1 ~ 4096)
     * @param temperature   응답 다양성 (0.0 ~ 1.0)
     * @param systemPrompt  시스템 프롬프트 (null 허용)
     * @param messages      메시지 리스트 (1개 이상)
     * @param errorStrategy 에러 처리 전략
     * @param fallbackValue fallback 전략 시 반환할 문자열 (nullable)
     * @param callerName    호출 서비스 식별명 (로깅용)
     * @return Bedrock 응답 텍스트
     */
    public String invoke(String modelId, int maxTokens, double temperature,
                         String systemPrompt, List<BedrockMessage> messages,
                         BedrockErrorStrategy errorStrategy, String fallbackValue,
                         String callerName) {

        // 1. 파라미터 검증 (Fail-Fast)
        validateParameters(modelId, maxTokens, temperature, messages, callerName);

        // 2. JSON payload 구성
        JSONObject payload = buildPayload(maxTokens, temperature, systemPrompt, messages);

        // 3. 요청 빌드 및 호출 + 에러 처리
        try {
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);

            // 4. response body에서 content[0].text 추출
            String responseBody = response.body().asUtf8String();
            return extractText(responseBody);

        } catch (Exception e) {
            // 5. 예외 발생 시 errorStrategy에 따라 처리
            return handleError(e, errorStrategy, fallbackValue, callerName, modelId);
        }
    }

    private void validateParameters(String modelId, int maxTokens, double temperature,
                                    List<BedrockMessage> messages, String callerName) {
        if (modelId == null || modelId.isBlank()) {
            throw new BedrockInvocationException(callerName, "unknown",
                    "modelId는 null이거나 빈 문자열일 수 없습니다.");
        }
        if (maxTokens < 1 || maxTokens > 4096) {
            throw new BedrockInvocationException(callerName, modelId,
                    String.format("maxTokens는 1~4096 범위여야 합니다. (입력값: %d)", maxTokens));
        }
        if (temperature < 0.0 || temperature > 1.0) {
            throw new BedrockInvocationException(callerName, modelId,
                    String.format("temperature는 0.0~1.0 범위여야 합니다. (입력값: %.2f)", temperature));
        }
        if (messages == null || messages.isEmpty()) {
            throw new BedrockInvocationException(callerName, modelId,
                    "messages는 1개 이상이어야 합니다.");
        }
    }

    private JSONObject buildPayload(int maxTokens, double temperature,
                                    String systemPrompt, List<BedrockMessage> messages) {
        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            payload.put("system", systemPrompt);
        }

        JSONArray msgArray = new JSONArray();
        for (BedrockMessage msg : messages) {
            msgArray.put(new JSONObject()
                    .put("role", msg.role())
                    .put("content", msg.content()));
        }
        payload.put("messages", msgArray);

        return payload;
    }

    private String extractText(String responseBody) {
        try {
            return new JSONObject(responseBody)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            log.error("[BedrockClientFacade] 응답 파싱 실패 | responseBody(앞 200자)={}",
                    responseBody.substring(0, Math.min(200, responseBody.length())));
            throw new RuntimeException("Bedrock 응답 파싱 중 오류가 발생했습니다.", e);
        }
    }

    private String handleError(Exception e, BedrockErrorStrategy errorStrategy,
                               String fallbackValue, String callerName, String modelId) {
        // 구조화된 에러 로깅: 호출 서비스명, model ID, 예외 클래스, 실패 원인 메시지
        log.error("[BedrockClientFacade] Bedrock 호출 실패 | caller={} | model={} | exceptionType={} | message={}",
                callerName, modelId, e.getClass().getSimpleName(), e.getMessage(), e);

        if (errorStrategy == BedrockErrorStrategy.RETURN_FALLBACK) {
            log.warn("[BedrockClientFacade] FALLBACK 전략 적용 | caller={} | fallback='{}'",
                    callerName, fallbackValue);
            return fallbackValue;
        }

        // THROW_EXCEPTION 전략: 원본 예외를 cause로 감싸서 전파
        throw new BedrockInvocationException(callerName, modelId,
                "Bedrock API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
}
