package com.dev.dugout.infrastructure.aws.s3;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.master-key}")
    private String masterKey;

    public String fetchMasterJson() {
        try {
            log.info("====> [S3] 마스터 데이터 로드 시도 (Bucket: {}, Key: {})", bucketName, masterKey);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(masterKey)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            return objectBytes.asString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("#### [S3 ERROR] 마스터 파일 로드 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}