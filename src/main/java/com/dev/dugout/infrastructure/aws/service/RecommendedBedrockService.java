package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendedBedrockService {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    public String generateBatchReason(List<TeamRecommendationResponseDto> teams, String userPreference) {
        // 1. 3개 팀의 데이터를 문자열로 결합 (필수 변경 사항)
        StringBuilder teamDataBuilder = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            TeamRecommendationResponseDto t = teams.get(i);
            teamDataBuilder.append(String.format(
                    "[%d순위 추천 후보]\n- 추천 팀: %s (%s년)\n- 팀 기록: %s\n\n",
                    i + 1, t.getTeamName(), t.getYear(), t.getStatsSummary().replace(".0", "")
            ));
        }

        // 2. 기존 프롬프트의 틀을 유지하며 데이터 부분만 교체
        String prompt = String.format(
                "너는 야구 입문자에게 객관적인 지표를 분석하여 최적의 팀을 추천하는 전문적인 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 제공된 데이터를 분석하여 사용자에게 상위 3개 팀을 순위별로 추천하고 있어.\n\n" +

                        "### [규칙 1] 대화 에티켓 ###\n" +
                        "1. **첫 인사**: 반드시 '안녕하세요, 더그아웃 스카우터 입니다. 반갑습니다.'로 시작하십시오. (전체 답변에서 딱 한 번만 수행)\n" +
                        "2. **격식체**: 문장은 반드시 '~합니다', '~입니다'로 끝맺고 전문가의 정중함을 유지하십시오.\n\n" +

                        "### [규칙 2] KBO 공식 팀명 절대 엄수 (최우선 순위) ###\n" +
                        "- 본문에서는 **무조건 아래의 풀네임 리스트 중 하나로만** 지칭하십시오.\n" +
                        "- **공식 명칭 리스트**: [KIA 타이거즈, 삼성 라이온즈, LG 트윈스, 두산 베어스, kt wiz, SSG 랜더스, 한화 이글스, 롯데 자이언츠, NC 다이노스, 키움 히어로즈]\n" +
                        "- **절대 금지**: 줄임말이나 '구단', '팀' 등의 접미사 사용을 엄격히 금지합니다.\n\n" +

                        "### [규칙 3] 가독성 및 강조 규칙 ###\n" +
                        "1. **구성**: 추천된 1위, 2위, 3위 팀 각각에 대해 아래 3개 문단 구조를 반복하여 작성하십시오.\n" +
                        "   - 1문단: 사용자의 취향(%2$s)과 해당 팀 선택의 개연성 설명\n" +
                        "   - 2문단: 제공된 기록을 인용한 구체적인 기술적 분석\n" +
                        "   - 3문단: 스카우터로서 해당 팀이 선사할 가치 제언\n" +
                        "2. **강조 표시**: 팀명과 핵심 지표 수치는 반드시 ** 강조할 내용 ** 처럼 별표 앞뒤에 공백을 두어 강조하십시오.\n\n" +

                        "### [추천 후보 데이터] ###\n%1$s" +
                        "- 사용자 취향: %2$s\n\n" +

                        "자, 전문가로서 위 데이터를 분석하여 1위부터 3위까지 각 팀의 추천 근거를 상세히 설명해 주십시오.",
                teamDataBuilder.toString(), userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 2500); // 3팀 분석을 위해 토큰 수 상향 (필수)
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

            return resp.getJSONArray("content").getJSONObject(0).getString("text").trim();

        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] : {}", e.getMessage());
            return "데이터 분석 중 오류가 발생했습니다. 하지만 추천된 팀들은 모두 KBO 역사에 남을 명팀들입니다. 상세 기록을 확인해 보시기 바랍니다.";
        }
    }
}