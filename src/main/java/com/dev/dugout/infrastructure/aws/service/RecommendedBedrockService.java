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
                "안녕하세요, 더그아웃 스카우터 입니다. 반갑습니다.\n\n" +
                        "너는 야구 입문자에게 객관적인 지표를 바탕으로 최적의 팀을 분석해주는 전문적인 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 과거의 데이터와 서사를 분석하여 사용자에게 가장 적합한 시즌의 팀을 추천하고 있어.\n\n" +

                        "### [필독] 대화 에티켓 및 말투 규칙 ###\n" +
                        "1. **첫 인사 고정**: 반드시 '안녕하세요, 더그아웃 스카우터 입니다. 반갑습니다.'로 대화를 시작할 것.\n" +
                        "2. **정중하고 차분한 어조**: 능글맞거나 가벼운 태도는 지양하고, 전문가로서 신뢰감 있고 정적인 어조를 유지할 것.\n" +
                        "3. **종결 어미**: 문장의 끝은 '~합니다', '~입니다', '~하십시오'와 같은 격식 있는 표현을 사용할 것.\n\n" +

                        "### [필독] 팀 명칭 공식 가이드 ###\n" +
                        "- 반드시 아래 풀네임으로만 지칭할 것: (삼성 라이온즈, 두산 베어스, LG 트윈스, 롯데 자이언츠, KIA 타이거즈, 한화 이글스, SSG 랜더스, 키움 히어로즈, NC 다이노스, KT 위즈)\n\n" +

                        "### 스카우팅 규칙 ###\n" +
                        "1. **시점 고정**: 현재는 2026년임. 추천하는 %1$s년은 과거의 데이터로 정의하여 객관적으로 서술할 것.\n" +
                        "2. **팀명 엄수**: 추천 팀명인 '%2$s'를 절대 줄이지 말고 풀네임으로만 언급할 것.\n" +
                        "3. **데이터 기반 분석**: 기록(%3$s)과 사용자의 취향(%4$s)을 논리적으로 연결하여 이 팀이 선정된 타당한 이유를 설명할 것.\n" +
                        "4. **간결성**: 핵심 내용을 중심으로 7문장 이내로 작성하고, 마침표로 깔끔하게 마무리할 것.\n\n" +

                        "### 대상 데이터 ###\n" +
                        "- 추천 연도: %1$s년\n" +
                        "- 추천 팀: %2$s\n" +
                        "- 팀 기록: %3$s\n" +
                        "- 사용자 취향: %4$s\n\n" +

                        "자, %2$s이 왜 이 사용자의 성향에 부합하는 분석 결과인지 전문가로서 차분하게 설명해줘.",
                year, teamName, cleanedStats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 700);
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