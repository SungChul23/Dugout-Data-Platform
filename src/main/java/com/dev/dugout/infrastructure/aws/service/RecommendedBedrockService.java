package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.infrastructure.aws.bedrock.BedrockClientFacade;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockErrorStrategy;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockMessage;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendedBedrockService {

    private final BedrockClientFacade bedrockClientFacade;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String CALLER_NAME = "RecommendedBedrockService";
    private static final String FALLBACK_MESSAGE = "데이터 분석 중 오류가 발생했습니다. 하지만 추천된 팀들은 모두 KBO 역사에 남을 명팀들입니다. 상세 기록을 확인해 보시기 바랍니다.";

    public String generateBatchReason(List<TeamRecommendationResponseDto> teams, String userPreference) {
        // 1. 3개 팀의 데이터를 문자열로 결합
        StringBuilder teamDataBuilder = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            TeamRecommendationResponseDto t = teams.get(i);
            teamDataBuilder.append(String.format(
                    "[%d순위 추천 후보]\n- 추천 팀: %s (%s년)\n- 팀 기록: %s\n\n",
                    i + 1, t.getTeamName(), t.getYear(), t.getStatsSummary().replace(".0", "")
            ));
        }

        // 2. 프롬프트 구성
        String prompt = String.format(
                "너는 야구 입문자에게 객관적인 지표를 분석하여 최적의 팀을 추천하는 전문적인 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고, 너는 제공된 데이터를 분석하여 사용자에게 상위 3개 팀을 순위별로 추천하고 있어.\n\n" +

                        "### [규칙 1] 대화 에티켓 ###\n" +
                        "1. **첫 인사**: 반드시 '안녕하세요, 더그아웃 스카우터 입니다. 반갑습니다.'로 시작하십시오.\n" +
                        "2. **격식체**: 문장은 반드시 '~합니다', '~입니다'로 끝맺고 전문가의 정중함을 유지하십시오.\n\n" +

                        "### [규칙 2] KBO 공식 팀명 절대 엄수 ###\n" +
                        "- 본문에서는 무조건 공식 풀네임 리스트 중 하나로만 지칭하십시오.\n\n" +

                        "### [규칙 3] 마크다운 가독성 규칙 (중요) ###\n" +
                        "1. **제목 강조**: 각 순위의 시작은 반드시 `## [n위 추천] 팀명` 형식을 사용하여 제목 크기를 키워주십시오.\n" +
                        "   - 예시: `## [1위 추천] 삼성 라이온즈` \n" +
                        "2. **구분선**: 각 순위별 추천 내용이 끝날 때마다 가로 구분선 `---` 을 넣어 섹션을 명확히 분리하십시오.\n" +
                        "3. **문단 구성**: 각 팀당 아래 3개 문단을 유지하십시오.\n" +
                        "   - 1문단: 사용자 취향(%2$s)과의 연관성\n" +
                        "   - 2문단: 기록(%1$s) 기반 기술 분석\n" +
                        "   - 3문단: 스카우터의 제언\n" +
                        "4. **강조**: 핵심 수치는 반드시 ** 강조할 내용 ** 처럼 별표와 공백을 사용하여 강조하십시오.\n\n" +

                        "### [추천 후보 데이터] ###\n%1$s" +
                        "- 사용자 취향: %2$s\n\n" +

                        "자, 전문가로서 위 데이터를 분석하여 1위부터 3위까지 추천 근거를 상세히 작성해 주십시오.",
                teamDataBuilder.toString(), userPreference
        );

        // 3. BedrockClientFacade를 통한 통합 호출
        String result = bedrockClientFacade.invoke(
                MODEL_ID, 2500, 0.7,
                null,
                List.of(BedrockMessage.user(prompt)),
                BedrockErrorStrategy.RETURN_FALLBACK,
                FALLBACK_MESSAGE,
                CALLER_NAME
        );

        return result != null ? result.trim() : FALLBACK_MESSAGE;
    }
}
