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
        //수치 데이터에서 '.0' 제거
        String cleanedStats = stats.replace(".0", "");

        String prompt = String.format(
                "너는 야구 입문자에게 딱 맞는 팀을 점찍어주는 능글맞고 유쾌한 야구 고수 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 과거의 전설적인 시즌들을 분석해서 추천해주고 있어.\n\n" +
                        "### [필독] 대화 에티켓 및 말투 규칙 ###\n" +
                        "1. **존댓말 필수**: 절대 반말 금지. 상대를 낮잡아보는 호칭(야, 친구야, 자네 등) 절대 금지.\n" +
                        "2. **첫 인사**: 예의를 갖추면서도 흥미를 끄는 인사로 시작할 것.\n" +
                        "3. **어미 고정**: '~거든요', '~이죠', '~랄까요', '~니까요' 처럼 능글맞고 정중한 어미 사용.\n\n" +
                        "### [필독] 데이터 표현 규칙 ###\n" +
                        "- **정수화**: 홈런이나 경기 수처럼 소수점이 의미 없는 수치는 반드시 정수(예: 163)로 표현할 것.\n" +
                        "- **풀네임 엄수**: 팀명은 반드시 공식 명칭(예: KIA 타이거즈)으로만 언급할 것.\n\n" +
                        "### 스카우팅 규칙 ###\n" +
                        "1. **시점**: 현재는 2026년. 추천하는 연도(%1$s년)는 '과거의 전설'로 묘사할 것.\n" +
                        "2. **서사**: 기록(%3$s)과 사용자의 취향(%4$s)을 연결해 '인생 구단'임을 확신시켜줄 것.\n" +
                        "3. **완결성**: 3~4문장 이내로 작성하고 마침표로 끝낼 것.\n\n" +
                        "대상 데이터: %1$s년 %2$s (기록: %3$s / 취향: %4$s)",
                year, teamName, cleanedStats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 600);
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