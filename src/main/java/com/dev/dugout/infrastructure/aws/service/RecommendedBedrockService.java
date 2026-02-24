package com.dev.dugout.infrastructure.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendedBedrockService {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    public String generateReason(String teamName, String year, String stats, String userPreference) {
        String cleanedStats = stats.replace(".0", "");

        String prompt = String.format(
                "너는 야구 입문자에게 객관적인 지표를 분석하여 최적의 팀을 추천하는 전문적인 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 과거 %1$s년의 데이터와 서사를 분석하여 사용자에게 팀을 추천하고 있어.\n\n" +

                        "### [규칙 1] 대화 및 가독성 규칙 (필독) ###\n" +
                        "1. **첫 인사**: 반드시 '안녕하세요, 더그아웃 스카우터 입니다. 반갑습니다.'로 시작할 것.\n" +
                        "2. **문단 나누기**: 가독성을 위해 전체 내용을 아래 3개 문단으로 구분하여 작성할 것 (각 문단 사이 공백 한 줄 필수).\n" +
                        "   - 1문단: 사용자의 취향과 팀 선택의 이유\n" +
                        "   - 2문단: 제공된 기록 수치를 인용한 기술적 분석\n" +
                        "   - 3문단: 스카우터로서의 최종 제언\n" +
                        "3. **강조 표시**: 추천 팀명(%2$s)과 핵심 기록 수치는 반드시 `**`를 사용하여 강조할 것.\n\n" +

                        "### [규칙 2] 팀 명칭 가이드 ###\n" +
                        "- 반드시 풀네임(%2$s)으로만 지칭하십시오. (줄임말, '구단', '팀' 등의 접미사 절대 금지)\n\n" +

                        "### [규칙 3] 스카우팅 분석 로직 ###\n" +
                        "1. **데이터 매칭**: 사용자의 취향(%4$s)을 충족시키는 기록(%3$s)을 논리적으로 연결하십시오.\n" +
                        "2. **시점 유지**: 현재는 2026년이며, %1$s년은 영광스러운 과거의 기록으로 서술하십시오.\n" +
                        "3. **분량**: 핵심 내용을 중심으로 7문장 이내로 작성하십시오.\n\n" +

                        "### [대상 데이터] ###\n" +
                        "- 추천 연도: %1$s년\n" +
                        "- 추천 팀: %2$s\n" +
                        "- 팀 기록: %3$s\n" +
                        "- 사용자 취향: %4$s\n\n" +

                        "전문가로서 위 데이터를 분석하여 차분하고 논리적으로 설명해 주십시오.",
                year, teamName, cleanedStats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 1000);
        payload.put("temperature", 0.7);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        try {
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());

            // Claude 3 모델은 응답 구조가 'content' 배열 내에 'text' 필드가 있음
            return resp.getJSONArray("content").getJSONObject(0).getString("text").trim();

        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] : {}", e.getMessage());
            return String.format("%s년 %s은 정말 대단한 팀이었거든요! 직접 확인해 보시면 깜짝 놀라실 거예요.", year, teamName);
        }
    }
}