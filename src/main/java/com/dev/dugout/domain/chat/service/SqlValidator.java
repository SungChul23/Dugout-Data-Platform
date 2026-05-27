package com.dev.dugout.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SqlValidator {

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
            "TRUNCATE", "CREATE", "REPLACE INTO", "MERGE",
            "EXEC", "EXECUTE", "CALL", "GRANT", "REVOKE",
            "users", "refresh_tokens", "forbidden_words",
            "user_dashboard", "user_prediction_limit"
    );

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 생성되지 않았습니다.");
        }

        String upperSql = sql.trim().toUpperCase();

        // SELECT로 시작하는지 확인
        if (!upperSql.startsWith("SELECT")) {
            log.warn("[SqlValidator] SELECT가 아닌 SQL 감지: {}", sql);
            throw new IllegalArgumentException("SELECT 쿼리만 허용됩니다.");
        }

        // 위험 키워드 차단
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword.toUpperCase())) {
                log.warn("[SqlValidator] 위험 키워드 감지: {} / SQL: {}", keyword, sql);
                throw new IllegalArgumentException("허용되지 않는 SQL 키워드가 포함되어 있습니다: " + keyword);
            }
        }

        // 화이트리스트 테이블 검증
        boolean hasAllowedTable = ALLOWED_TABLES.stream()
                .anyMatch(table -> upperSql.contains(table.toUpperCase()));
        if (!hasAllowedTable) {
            log.warn("[SqlValidator] 허용되지 않은 테이블 접근 시도: {}", sql);
            throw new IllegalArgumentException("허용되지 않은 테이블입니다.");
        }

        // 다중 쿼리 방지
        long semicolonCount = sql.chars().filter(c -> c == ';').count();
        if (semicolonCount > 1) {
            log.warn("[SqlValidator] 다중 쿼리 감지: {}", sql);
            throw new IllegalArgumentException("다중 쿼리는 허용되지 않습니다.");
        }

        log.info("[SqlValidator] SQL 검증 통과");
    }

    /**
     * LLM 응답에서 SQL만 추출
     * 케이스 1: ```sql ... ``` 마크다운 블록
     * 케이스 2: ``` ... ``` 마크다운 블록
     * 케이스 3: SELECT 키워드 직접 탐색 (앞뒤 텍스트 제거)
     */
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

        // 케이스 3: SELECT 키워드 직접 탐색
        // >= 0 으로 SELECT가 맨 앞(index=0)인 경우도 처리
        int selectIndex = cleaned.toUpperCase().indexOf("SELECT");
        if (selectIndex >= 0) {
            if (selectIndex > 0) {
                log.info("[SqlValidator] SELECT 앞 불필요한 텍스트 제거");
            }
            cleaned = cleaned.substring(selectIndex);

            // 세미콜론 이후 텍스트 항상 제거
            int semicolonIndex = cleaned.indexOf(';');
            if (semicolonIndex >= 0) {
                cleaned = cleaned.substring(0, semicolonIndex + 1);
                log.info("[SqlValidator] 세미콜론 이후 텍스트 제거");
            }
            return cleaned.trim();
        }

        return cleaned.trim();
    }
}