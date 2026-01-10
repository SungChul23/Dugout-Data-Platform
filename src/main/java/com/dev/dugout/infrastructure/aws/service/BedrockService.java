package com.dev.dugout.infrastructure.aws.service;

import lombok.RequiredArgsConstructor;
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
public class BedrockService {

    private final BedrockRuntimeClient client = BedrockRuntimeClient.builder()
            .region(Region.AP_NORTHEAST_2) // 서울 리전
            .build();

    public String generateReason(String teamName, String year, String stats, String userPreference) {
        // [가드레일 및 페르소나 강화 프롬프트]
        String prompt = String.format(
                "너는 야구 데이터를 기반으로 팀을 추천해주는 다정하고 친근한 친구 '더그아웃 AI'야.\n" +
                        "아래 규칙을 반드시 지켜서 답변해줘.\n\n" +
                        "### 규칙 ###\n" +
                        "1. 말투: '~해요', '~네요' 체를 사용하여 아주 친근하고 다정하게 말할 것.\n" +
                        "2. 가드레일: 제공된 데이터(기록, 취향) 안에서만 이야기하고, 없는 사실을 지어내지 말 것.\n" +
                        "3. 길이: 반드시 3문장 이내로 짧고 강렬하게 작성할 것.\n" +
                        "4. 금지사항: 정치, 사회적 이슈 등 야구 외의 주제는 절대 언급하지 말 것.\n" +
                        "5. 핵심: 사용자의 '취향'과 팀의 '기록'이 어떻게 연결되는지 한 눈에 알게 해줄 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀: %s년 %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이제 성철 님(사용자)에게 왜 이 팀이 딱인지 말해줘!",
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
                    .modelId("anthropic.claude-3-haiku-20240307")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            return "와우! 성철 님과 정말 잘 어울리는 팀을 찾았어요. 데이터가 증명하는 이 팀의 매력을 직접 확인해보세요! ⚾️";
        }
    }
}