package com.dev.dugout.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SqlValidator {

    // 추후 관리가 어려워지면 화이트리스트로 선택하자
    private static final List<String> ALLOWED_TABLES = List.of(
            "daily_player_hitter",
            "daily_player_pitcher",
            "daily_team_ranking",
            "daily_team_stats",
            "game",
            "prediction_result",
            "fa_market",
            "gg_leaderboard",
            "player",
            "team"
    );

    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER",
            "TRUNCATE", "CREATE", "REPLACE", "MERGE",
            "EXEC", "EXECUTE", "CALL", "GRANT", "REVOKE",
            "users", "refresh_tokens", "forbidden_words",
            "user_dashboard", "user_prediction_limit"
    );

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 생성되지 않았습니다.");
        }

        String upperSql = sql.trim().toUpperCase();

        if (!upperSql.startsWith("SELECT")) {
            log.warn("[SqlValidator] SELECT가 아닌 SQL 감지: {}", sql);
            throw new IllegalArgumentException("SELECT 쿼리만 허용됩니다.");
        }

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword.toUpperCase())) {
                log.warn("[SqlValidator] 위험 키워드 감지: {} / SQL: {}", keyword, sql);
                throw new IllegalArgumentException("허용되지 않는 SQL 키워드가 포함되어 있습니다: " + keyword);
            }
        }

        long semicolonCount = sql.chars().filter(c -> c == ';').count();
        if (semicolonCount > 1) {
            log.warn("[SqlValidator] 다중 쿼리 감지: {}", sql);
            throw new IllegalArgumentException("다중 쿼리는 허용되지 않습니다.");
        }

        log.info("[SqlValidator] SQL 검증 통과");
    }

    // LLM 응답에서 SQL만 추출
    // 케이스 1: ```sql ... ``` 마크다운 블록
    // 케이스 2: SQL 앞뒤에 설명 텍스트가 붙은 경우 → SELECT 위치 직접 탐색
    public String extractSql(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String cleaned = rawResponse.trim();

        // 케이스 1: ```sql 블록
        if (cleaned.contains("```sql")) {
            int start = cleaned.indexOf("```sql") + 6;
            int end = cleaned.lastIndexOf("```");
            if (end > start) {
                return cleaned.substring(start, end).trim();
            }
        }

        // 케이스 2: ``` 블록
        if (cleaned.contains("```")) {
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.lastIndexOf("```");
            if (end > start) {
                return cleaned.substring(start, end).trim();
            }
        }

        // 케이스 3: SQL 앞뒤에 설명 텍스트가 붙은 경우
        // → 대소문자 무관하게 SELECT 키워드 위치 탐색
        int selectIndex = cleaned.toUpperCase().indexOf("SELECT");
        if (selectIndex > 0) {
            log.info("[SqlValidator] SELECT 앞에 불필요한 텍스트 감지 → 자동 제거");
            cleaned = cleaned.substring(selectIndex);

            // 세미콜론 이후 텍스트 제거
            int semicolonIndex = cleaned.indexOf(';');
            if (semicolonIndex >= 0) {
                cleaned = cleaned.substring(0, semicolonIndex + 1);
            }
            return cleaned.trim();
        }

        return cleaned.trim();
    }
}