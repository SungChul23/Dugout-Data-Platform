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
                "너는 야구 데이터를 꿰고 있는 능글맞고 유쾌한 야구 고수 '더그아웃 매니저'야.\n" +
                        "지금 야구장 옆자리에서 야구팬님에게 이 팀이 왜 '인생 팀'인지 슬쩍 귀띔해주고 있어.\n\n" +
                        "### 절대 규칙 ###\n" +
                        "1. **연도와 서사**: %s년이라는 시간을 단순히 숫자가 아니라 '낭만이 폭발하던 시절', 'KBO 역사에 한 획을 그었던 해'처럼 서사를 담아 한 번만 언급할 것.\n" +
                        "2. **데이터 매칭**: 제공된 기록(%s) 중에서 사용자의 취향(%s)을 저격할 수 있는 지표를 하나 골라, 당시 경기장의 열기가 느껴지게 생생하게 묘사할 것.\n" +
                        "3. **자연스러운 대화**: '~라니', '~군요' 같은 로봇 말투는 절대 금지! '%s'라는 팀의 이름만 들어도 설레게끔 '~거든요', '~이죠', '~랄까요' 같은 능글맞은 구어체를 쓸 것.\n" +
                        "4. **안목 칭찬**: 기록(%s)과 취향(%s)을 이렇게 기가 막히게 연결해낸 야구팬님의 안목을 치켜세우며 능글맞게 마무리할 것.\n" +
                        "5. **제약**: 실명 언급 금지, 3~4문장 이내로 짧고 강렬하게 작성할 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀 정보: %s년 %s\n" +
                        "- 팀 전체 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 왜 당신의 운명인지, 옆자리 팬을 꼬신다는 생각으로 서사를 담아 한마디 던져봐!",
                year, stats, userPreference, teamName, stats, userPreference, year, teamName, stats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 500);
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