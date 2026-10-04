package com.dev.dugout.domain.postseason.service;

import com.dev.dugout.infrastructure.aws.bedrock.BedrockClientFacade;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockErrorStrategy;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostseasonSummaryBedrockService {

    private final BedrockClientFacade bedrockClientFacade;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String CALLER_NAME = "PostseasonSummaryBedrockService";

    private static final String SYSTEM_PROMPT = """
            너는 KBO 구단의 정규시즌을 요약하는 야구 전문 에디터다.

            [규칙]
            1. [구단 데이터]에 있는 수치와 선수 이름만 사용한다. 데이터에 없는 기록, 선수, 사건, 감독, 부상, 트레이드는 절대 쓰지 않는다.
            2. 수치는 주어진 값 그대로 쓴다. 반올림하거나 새로 계산하지 않는다.
            3. 포스트시즌 결과나 전망은 쓰지 않는다. 정규시즌 내용만 다룬다.
            4. 2~3문장, 공백 포함 150자 이내의 평서문으로 쓴다.
            5. 첫 문장은 "{시즌} 정규시즌 {승}승 {패}패 {무}무(승률 {승률})로 정규시즌 {순위}위 확정." 형식으로 시작한다.
            6. 이어서 팀의 가장 두드러진 강점 하나(리그 순위가 가장 높은 지표)와 핵심 선수 2~3명을 언급한다.
            7. 인사말, 마크다운, 따옴표, 이모지 없이 요약 본문만 출력한다.

            [예시 출력]
            2026 정규시즌 78승 65패 1무(승률 0.543)로 정규시즌 2위 확정. 팀 홈런 185개로 리그 1위에 오른 장타력이 강점이며, 구자욱·디아즈·김영웅이 타선을 이끌었다.
            """;

    // 실패 시 fallback 문구를 DB에 저장하지 않도록 null을 반환한다
    public String generateSummary(String teamData) {
        String result = bedrockClientFacade.invoke(
                MODEL_ID, 300, 0.3,
                SYSTEM_PROMPT,
                List.of(BedrockMessage.user("[구단 데이터]\n" + teamData)),
                BedrockErrorStrategy.RETURN_FALLBACK,
                null,
                CALLER_NAME
        );

        return result != null && !result.isBlank() ? result.trim() : null;
    }
}
