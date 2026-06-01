package com.dev.dugout.global.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3JsonReader {

    private final S3Client s3Client; // AWS SDK 설정 필요
    private final ObjectMapper objectMapper; // 스프링이 제공하는 JSON 변환기

    @Value("${aws.s3.bucket}")
    private String bucketName;


    //S3에서 JSON 파일을 읽어 지정한 클래스의 리스트로 반환
    public <T> List<T> read(String s3Path, Class<T> clazz) {
        // .parquet 경로가 들어와도 .json으로 바꿔서 읽도록 처리
        String jsonPath = s3Path.replace(".parquet", ".json");
        log.info("S3 JSON 데이터 읽기 시작: {}", jsonPath);

        try {
            // 1. S3에 파일 요청
            ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(jsonPath)
                    .build());

            // 2. Jackson ObjectMapper로 JSON 데이터를 List<DTO>로 변환
            return objectMapper.readValue(s3is,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));

        } catch (IOException e) {
            log.error("S3 JSON 역직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("데이터 입고 중 오류 발생", e);
        }
    }
}