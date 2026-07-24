package com.dev.dugout.domain.chat.service;

import com.dev.dugout.infrastructure.aws.bedrock.BedrockClientFacade;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockErrorStrategy;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBedrockService {

    private final BedrockClientFacade bedrockClientFacade;
    private final PromptLoader promptLoader;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String CALLER_NAME = "ChatBedrockService";

    /**
     * 1차 Bedrock 호출 - SQL 생성
     */
    public String generateSql(String schema, String conversationHistory, String question) {
        log.info("[ChatBedrockService] SQL 생성 요청: {}", question);

        String systemPrompt = promptLoader.get(PromptLoader.SQL_SYSTEM);
        String fewShot      = promptLoader.get(PromptLoader.SQL_FEWSHOT);

        String prompt = String.format(
                "%s\n\n[DB 스키마]\n%s\n\n%s\n\n[대화 히스토리]\n%s\n\n[현재 질문]\n%s\n\nSQL:",
                systemPrompt,
                schema,
                fewShot,
                conversationHistory,
                question
        );

        return bedrockClientFacade.invoke(
                MODEL_ID, 500, 0.0,
                null,
                List.of(BedrockMessage.user(prompt)),
                BedrockErrorStrategy.THROW_EXCEPTION,
                null,
                CALLER_NAME
        ).trim();
    }

    /**
     * 2차 Bedrock 호출 - 자연어 답변 생성
     */
    public String generateAnswer(String question, List<Map<String, Object>> dbResult) {
        log.info("[ChatBedrockService] 자연어 변환 요청");

        String systemPrompt = promptLoader.get(PromptLoader.ANS_SYSTEM);

        String resultStr = dbResult.isEmpty()
                ? "조회 결과가 없습니다."
                : dbResult.toString();

        String prompt = String.format(
                "%s\n\n[오늘 날짜]\n%s\n\n[사용자 질문]\n%s\n\n[DB 조회 결과]\n%s\n\n답변:",
                systemPrompt,
                LocalDate.now(),
                question,
                resultStr
        );

        return bedrockClientFacade.invoke(
                MODEL_ID, 600, 0.7,
                null,
                List.of(BedrockMessage.user(prompt)),
                BedrockErrorStrategy.THROW_EXCEPTION,
                null,
                CALLER_NAME
        ).trim();
    }

    /**
     * 야구 도메인 질문인지 판별
     */
    public boolean isBaseballQuestion(String question) {
        List<String> nonBaseballKeywords = List.of(
                "주식", "코인", "부동산", "날씨", "맛집", "레시피",
                "쇼핑", "영화", "드라마", "음악", "정치", "선거",
                "의학", "병원", "법률", "세금"
        );

        for (String keyword : nonBaseballKeywords) {
            if (question.contains(keyword)) {
                log.info("[ChatBedrockService] 비야구 질문 감지: {}", keyword);
                return false;
            }
        }
        return true;
    }
}
