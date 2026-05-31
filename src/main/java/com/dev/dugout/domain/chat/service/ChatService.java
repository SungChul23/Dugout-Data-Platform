package com.dev.dugout.domain.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 챗봇 흐름 전체 오케스트레이션
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {


    private final ChatInputValidator chatInputValidator;
    private final SchemaRouter schemaRouter;
    private final LlmSchemaRouter llmSchemaRouter;
    private final SchemaContextBuilder schemaContextBuilder;
    private final SqlValidator sqlValidator;
    private final SqlExecutor sqlExecutor;
    private final ChatBedrockService chatBedrockService;

    // 대화 히스토리 캐시 (conversationId → 메시지 목록)
    // 30분 무활동 시 만료, 최대 500개 대화 세션
    private Cache<String, List<ConversationMessage>> conversationCache;

    private static final int MAX_HISTORY_SIZE = 10; // 최근 10개 메시지만 유지

    //서버 재시작 후 초기화
    @PostConstruct
    public void init() {
        conversationCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES) // ttl 설정 -> 대화 날림
                // 만약 서버가 늘어난다면 레디스로 교체 해야함

                .maximumSize(500) // 최대 500 세션
                .build();
        log.info("[ChatService] 대화 히스토리 캐시 초기화 완료");
    }

    //챗봇 메인 처리 메서드
    public String chat(String userMessage, String conversationId) {
        log.info("[ChatService] 질문 수신 | conversationId: {} | message: {}",
                conversationId, userMessage);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 1단계: 대화 히스토리 로드
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        List<ConversationMessage> history = getHistory(conversationId);

        // SQL 직접 입력 차단
        if (chatInputValidator.looksLikeSql(userMessage)) {
            String rejection = "SQL 쿼리는 직접 입력할 수 없습니다. 😅\n"
                    + "자연어로 질문해주세요!\n"
                    + "예) '현재 타율 상위 3명 보여줘', '홈런 1위 타자 누구야?' , '예상 골든글러브 유격수 1위 누구야?' ";
            saveHistory(conversationId, history, userMessage, rejection);
            return rejection;
        }

        // 민감 정보 요청 차단
        if (chatInputValidator.containsSensitiveRequest(userMessage)) {
            String rejection = "보안상 해당 요청은 처리할 수 없습니다. 🔒\n"
                    + "KBO 야구 데이터 관련 질문만 답변 가능합니다!";
            saveHistory(conversationId, history, userMessage, rejection);
            return rejection;
        }


        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2단계: 야구 도메인 질문인지 확인
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (!chatBedrockService.isBaseballQuestion(userMessage)) {
            String refusal = "저는 KBO 야구 관련 질문만 답변할 수 있습니다. ⚾\n" +
                    "선수 성적, 팀 순위, 경기 일정, FA 분석 등을 물어보세요!";
            saveHistory(conversationId, history, userMessage, refusal);
            return refusal;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 3단계: Schema Routing (하이브리드)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        List<String> selectedTables = schemaRouter.route(userMessage);

        if (selectedTables.isEmpty()) {
            // 하드코딩 실패 → LLM 라우팅
            log.info("[ChatService] LLM 라우팅으로 위임");
            selectedTables = llmSchemaRouter.route(userMessage);
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 4단계: 스키마 구성
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        String schema = schemaContextBuilder.buildSchema(selectedTables);
        String historyText = formatHistory(history);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 5단계: Bedrock 1차 호출 - SQL 생성
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        String rawSql = chatBedrockService.generateSql(schema, historyText, userMessage);

        if ("NO_SQL".equals(rawSql.trim())) {
            log.info("[ChatService] 단순 일상 대화 감지 (NO_SQL 우회)");
            String greetingAnswer = "안녕하세요! KBO 야구 데이터 전문 AI 어시스턴트 '더그아웃 AI'입니다. 팀 순위, 선수 성적, 일정 , 수상 정보, FA 등 궁금한 점을 편하게 물어보세요! ";

            // 대화 기록에 저장 후 바로 반환
            saveHistory(conversationId, history, userMessage, greetingAnswer);
            return greetingAnswer;
        }

        String sql = sqlValidator.extractSql(rawSql);
        log.info("[ChatService] 생성된 SQL: {}", sql);



        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 6단계: SQL 검증
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        try {
            sqlValidator.validate(sql);
        } catch (IllegalArgumentException e) {
            log.warn("[ChatService] SQL 검증 실패: {}", e.getMessage());
            String errorAnswer = "죄송합니다. 해당 질문에 대한 데이터를 조회할 수 없습니다.\n" +
                    "다른 방식으로 질문해 보시겠어요? ⚾";
            saveHistory(conversationId, history, userMessage, errorAnswer);
            return errorAnswer;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 7단계: RDS 실행
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        List<Map<String, Object>> dbResult;
        try {
            dbResult = sqlExecutor.execute(sql);
        } catch (Exception e) {
            log.error("[ChatService] DB 실행 오류: {}", e.getMessage());
            String errorAnswer = "데이터 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            saveHistory(conversationId, history, userMessage, errorAnswer);
            return errorAnswer;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 8단계: Bedrock 2차 호출 - 자연어 변환
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        String answer = chatBedrockService.generateAnswer(userMessage, dbResult);
        log.info("[ChatService] 최종 답변 생성 완료");

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 9단계: 히스토리 저장
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        saveHistory(conversationId, history, userMessage, answer);

        return answer;
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 히스토리 관리 메서드
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private List<ConversationMessage> getHistory(String conversationId) {
        return conversationCache.get(conversationId, k -> new ArrayList<>());
    }

    private void saveHistory(String conversationId, List<ConversationMessage> history,
                             String userMessage, String answer) {
        history.add(new ConversationMessage("user", userMessage));
        history.add(new ConversationMessage("assistant", answer));

        // 최근 N개만 유지 (토큰 절약)
        if (history.size() > MAX_HISTORY_SIZE) {
            List<ConversationMessage> trimmed = history.subList(
                    history.size() - MAX_HISTORY_SIZE, history.size()
            );
            history = new ArrayList<>(trimmed);
        }

        conversationCache.put(conversationId, history);
    }

    private String formatHistory(List<ConversationMessage> history) {
        if (history.isEmpty()) return "없음";

        return history.stream()
                .map(ConversationMessage::toPromptFormat)
                .collect(Collectors.joining("\n"));
    }
}