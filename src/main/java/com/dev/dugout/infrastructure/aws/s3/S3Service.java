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


    @Value("${aws.s3.master-bucket}")
    private String masterBucketName;

    @Value("${aws.s3.master-key}")
    private String batterMasterKey;

    @Value("${aws.s3.pitcher-master-key}")
    private String pitcherMasterKey;

    @Value("${aws.s3.fa-batter-master-key}")
    private String faBatterMasterKey;

    @Value("${aws.s3.fa-pitcher-master-key}")
    private String faPitcherMasterKey;

    public String fetchFaBatterMasterJson() {
        return fetchFromS3(faBatterMasterKey, "FA 타자");
    }

    public String fetchFaPitcherMasterJson() {
        return fetchFromS3(faPitcherMasterKey, "FA 투수");
    }

    public String fetchMasterJson() {
        return fetchFromS3(batterMasterKey, "타자");
    }

    public String fetchPitcherMasterJson() {
        return fetchFromS3(pitcherMasterKey, "투수");
    }

    private String fetchFromS3(String key, String type) {
        try {
            log.info("====> [S3] {} 마스터 데이터 로드 시도 (Bucket: {}, Key: {})", type, masterBucketName, key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(masterBucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            return objectBytes.asString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("#### [S3 ERROR] {} 마스터 파일 로드 중 오류 발생: {}", type, e.getMessage());
            return null;
        }
    }
}