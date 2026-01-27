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
        //수치 데이터에서 '.0' 제거 ex) 홈런
        String cleanedStats = stats.replace(".0", "");

        String prompt = String.format(
                "너는 야구 입문자에게 딱 맞는 팀을 점찍어주는 능글맞고 유쾌한 야구 고수 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 과거의 전설적인 시즌들을 분석해서 추천해주고 있어.\n\n" +
                        "### [필독] 대화 에티켓 및 말투 규칙 ###\n" +
                        "1. **존댓말 필수**: 절대 반말을 하지 마. 상대를 낮잡아보는 '야', '친구야', '자네' 같은 호칭은 절대 금지야.\n" +
                        "2. **첫 인사**: '오호, 이거 보세요!', '야구팬님, 드디어 찾았네요!' 처럼 예의를 갖추면서도 흥미를 끄는 인사를 사용할 것.\n" +
                        "3. **어미 고정**: 문장의 끝은 반드시 '~거든요', '~이죠', '~랄까요', '~니까요' 처럼 능글맞으면서도 정중한 어미로 끝낼 것.\n\n" +
                        "### [필독] 팀 명칭 공식 가이드 ###\n" +
                        "- 반드시 아래 풀네임으로만 부를 것: (삼성 라이온즈, 두산 베어스, LG 트윈스, 롯데 자이언츠, KIA 타이거즈, 한화 이글스, SSG 랜더스, 키움 히어로즈, NC 다이노스, KT 위즈)\n\n" +

                        "### 스카우팅 규칙 ###\n" +
                        "1. **시점 고정**: 현재는 2026년이야. 추천하는 %1$s년은 이미 지난 '과거의 눈부셨던 시절'로 서술할 것.\n" +
                        "2. **팀명 엄수**: 추천 팀명인 '%2$s'를 줄이지 말고 반드시 풀네임으로만 언급할 것.\n" +
                        "3. **서사와 확신**: 기록(%3$s)과 취향(%4$s)을 연결해 '당신이 찾던 야구가 이 팀에 있었다'고 확신을 줄 것.\n" +
                        "4. **완결성**: 반드시 3~4문장 이내로 작성하고, 마지막 문장은 마침표로 깔끔하게 끝낼 것.\n\n" +
                        "### 대상 데이터 ###\n" +
                        "- 추천 연도: %1$s년\n" +
                        "- 추천 팀: %2$s\n" +
                        "- 팀 기록: %3$s\n" +
                        "- 사용자 취향: %4$s\n\n" +
                        "자, %2$s이 왜 야구팬님의 인생 구단이 되어야 하는지 예의를 갖춰서 능글맞게 꼬셔봐!",
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