package com.dev.dugout.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class ChatInputValidator {

    // 직접 SQL 입력 감지
    public boolean looksLikeSql(String message) {
        String upper = message.trim().toUpperCase();
        boolean result = upper.startsWith("SELECT")
                || upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")
                || upper.startsWith("DROP")
                || upper.startsWith("CREATE")
                || upper.startsWith("ALTER")
                || upper.startsWith("TRUNCATE");

        if (result) log.warn("[ChatInputValidator] SQL 직접 입력 감지: {}", message);
        return result;
    }

    // 민감 정보 요청 감지
    public boolean containsSensitiveRequest(String message) {
        List<String> sensitiveKeywords = List.of(
                "users", "이메일", "email",
                "비밀번호", "password", "passwd",
                "refresh_token", "토큰", "token",
                "개인정보", "회원정보", "로그인정보"
        );
        String lower = message.toLowerCase();
        boolean result = sensitiveKeywords.stream()
                .anyMatch(lower::contains);

        if (result) log.warn("[ChatInputValidator] 민감 키워드 감지: {}", message);
        return result;
    }
}