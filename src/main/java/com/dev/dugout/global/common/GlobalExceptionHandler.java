package com.dev.dugout.global.common;

import com.dev.dugout.infrastructure.aws.bedrock.BedrockInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 전역 예외 핸들러.
 * 컨트롤러에서 처리되지 않은 예외를 일관된 JSON 형식으로 응답한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bedrock API 호출 실패 예외 처리.
     * THROW_EXCEPTION 전략을 사용하는 서비스(ChatBedrockService)에서 발생.
     */
    @ExceptionHandler(BedrockInvocationException.class)
    public ResponseEntity<Map<String, Object>> handleBedrockInvocationException(BedrockInvocationException e) {
        log.error("[GlobalExceptionHandler] BedrockInvocationException | caller={} | model={} | message={}",
                e.getCallerName(), e.getModelId(), e.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "AI 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    /**
     * 일반 RuntimeException 처리.
     * 예상치 못한 서버 에러에 대한 안전망.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("[GlobalExceptionHandler] RuntimeException | message={}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
