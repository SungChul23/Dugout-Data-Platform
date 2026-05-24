package com.dev.dugout.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

//역할: 보안 게이트키퍼
//SELECT로 시작하는지 확인
//DROP/DELETE/UPDATE 등 위험 키워드 차단
//민감 테이블(users 등) 접근 차단
//다중 쿼리(;; 두개) 차단
@Slf4j
@Component
public class SqlValidator {

    // 허용된 테이블 목록 (챗봇이 조회 가능한 테이블만)
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

    // 위험 키워드 (절대 허용 안 함)
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER",
            "TRUNCATE", "CREATE", "REPLACE", "MERGE",
            "EXEC", "EXECUTE", "CALL", "GRANT", "REVOKE",
            "users", "refresh_tokens", "forbidden_words",
            "user_dashboard", "user_prediction_limit"
    );

    // SQL 유효성 검증
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

        // 위험 키워드 포함 여부 확인
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword.toUpperCase())) {
                log.warn("[SqlValidator] 위험 키워드 감지: {} / SQL: {}", keyword, sql);
                throw new IllegalArgumentException("허용되지 않는 SQL 키워드가 포함되어 있습니다: " + keyword);
            }
        }

        // 세미콜론 여러 개 (다중 쿼리 방지)
        long semicolonCount = sql.chars().filter(c -> c == ';').count();
        if (semicolonCount > 1) {
            log.warn("[SqlValidator] 다중 쿼리 감지: {}", sql);
            throw new IllegalArgumentException("다중 쿼리는 허용되지 않습니다.");
        }

        log.info("[SqlValidator] SQL 검증 통과");
    }

    //LLM 응답에서 SQL만 추출
    //Bedrock이 마크다운 코드블록으로 감쌀 수 있음
    public String extractSql(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String cleaned = rawResponse.trim();

        // ```sql ... ``` 블록 제거
        if (cleaned.contains("```sql")) {
            int start = cleaned.indexOf("```sql") + 6;
            int end = cleaned.lastIndexOf("```");
            if (end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        } else if (cleaned.contains("```")) {
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.lastIndexOf("```");
            if (end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        }

        // SQL이 여러 줄인 경우 하나의 문자열로
        return cleaned.trim();
    }
}