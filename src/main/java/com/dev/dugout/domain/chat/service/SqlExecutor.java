package com.dev.dugout.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor

// 역할: 검증된 SQL을 실제 RDS에 실행

public class SqlExecutor {

    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_ROWS = 20; // 최대 반환 행 수

    //동적 SQL 실행 후 결과 반환
    //@return List<Map<String, Object>> 형태의 결과
    public List<Map<String, Object>> execute(String sql) {
        log.info("[SqlExecutor] SQL 실행: {}", sql);

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            if (result.isEmpty()) {
                log.info("[SqlExecutor] 조회 결과 없음");
                return List.of();
            }

            // 결과가 너무 많으면 제한
            if (result.size() > MAX_ROWS) {
                log.info("[SqlExecutor] 결과 {}건 → {}건으로 제한", result.size(), MAX_ROWS);
                result = result.subList(0, MAX_ROWS);
            }

            log.info("[SqlExecutor] 실행 완료: {}건 반환", result.size());
            return result;

        } catch (Exception e) {
            log.error("[SqlExecutor] SQL 실행 오류: {}", e.getMessage());
            throw new RuntimeException("데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}