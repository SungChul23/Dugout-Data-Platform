package com.dev.dugout.domain.chat.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptLoader {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    // 프롬프트 키 상수
    public static final String SQL_SYSTEM   = "chat-sql-system";
    public static final String SQL_FEWSHOT  = "chat-sql-fewshot";
    public static final String ANS_SYSTEM   = "chat-answer-system";

    private static final Map<String, String> PROMPT_KEYS = Map.of(
            SQL_SYSTEM,  "prompts/chat-sql-system.txt",
            SQL_FEWSHOT, "prompts/chat-sql-fewshot.txt",
            ANS_SYSTEM,  "prompts/chat-answer-system.txt"
    );

    // 인메모리 캐시
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[PromptLoader] S3에서 프롬프트 로드 시작");
        reload();
    }

    // 전체 프롬프트 재로드 (관리자 API에서 호출)
    public void reload() {
        PROMPT_KEYS.forEach((name, s3Key) -> {
            try {
                String content = fetchFromS3(s3Key);
                cache.put(name, content);
                log.info("[PromptLoader] 로드 완료: {}", name);
            } catch (Exception e) {
                log.error("[PromptLoader] 로드 실패: {} → {}", name, e.getMessage());
                // 기존 캐시 유지 (서버 장애 방지)
            }
        });
        log.info("[PromptLoader] 프롬프트 로드 완료 ({}/{})", cache.size(), PROMPT_KEYS.size());
    }

    public String get(String key) {
        String prompt = cache.get(key);
        if (prompt == null) {
            log.warn("[PromptLoader] 캐시 미스: {} → S3 재시도", key);
            try {
                prompt = fetchFromS3(PROMPT_KEYS.get(key));
                cache.put(key, prompt);
            } catch (Exception e) {
                log.error("[PromptLoader] 긴급 로드 실패: {}", e.getMessage());
                throw new RuntimeException("프롬프트 로드 실패: " + key);
            }
        }
        return prompt;
    }

    private String fetchFromS3(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
        return bytes.asString(StandardCharsets.UTF_8);
    }
}