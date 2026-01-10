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
public class BedrockService {

    private final BedrockRuntimeClient client = BedrockRuntimeClient.builder()
            .region(Region.AP_NORTHEAST_2) // 서울 리전
            .build();

    public String generateReason(String teamName, String year, String stats, String userPreference) {
        // [가드레일 및 페르소나 강화 프롬프트]
        String prompt = String.format(
                "너는 야구 데이터를 빠삭하게 꿰고 있는 능글맞은 고수 '더그아웃 매니저'야. " +
                        "지금 야구장 옆자리에서 야구팬님을 꼬시고 있다고 생각하고 답변해줘.\n\n" +
                        "### 규칙 ###\n" +
                        "1. 말투: '오호, 이거 보세요', '기막힌 선택이죠', '말해 뭐합니까' 같은 여유로운 말투를 쓸 것.\n" +
                        "2. 묘사: 숫자를 단순히 읽지 말고, 그 숫자가 경기장에서 어떤 '맛'으로 느껴지는지(예: 짜릿한 손맛, 숨 막히는 긴장감, 9회말의 로망) 생생하게 꼬실 것.\n" +
                        "3. 고수의 참견: 마지막엔 '이 팀을 선택한 당신의 안목이 탁월하다'는 뉘앙스의 한마디를 덧붙여줘.\n" +
                        "4. 가드레일: 선수 이름이나 역사적 별명은 억지로 지어내지 말고, 오직 '기록'과 '취향'의 궁합에만 집중할 것.\n" +
                        "5. 제약: 사용자의 실명 언급 금지! '야구팬님' 혹은 '당신'이라고 부르며 3~4문장 이내로 작성할 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀: %s년 %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 얼마나 '물건'인지 능글맞게 꼬셔봐!",
                year, teamName, stats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 250);
        payload.put("temperature", 0.6); // 안정성을 위해 살짝 낮춤

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        try {
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] 원인: {}", e.getMessage()); // 로그에 에러 찍기
            e.printStackTrace(); // 상세 스택트레이스 출력
            return "AI 분석 실패 원인: " + e.getMessage();
        }
    }
}