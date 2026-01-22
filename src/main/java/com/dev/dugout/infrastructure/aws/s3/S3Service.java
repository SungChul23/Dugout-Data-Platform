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

    @Value("${aws.s3.master-key}") // 기존 타자 마스터 키
    private String masterKey;

    @Value("${aws.s3.pitcher-master-key}") // 투수용 마스터 키 추가
    private String pitcherMasterKey;

    //타자 마스터 데이터 로드
    public String fetchMasterJson() {
        return fetchFromS3(masterKey, "타자");
    }

    //투수 마스터 데이터 로드
    public String fetchPitcherMasterJson() {
        return fetchFromS3(pitcherMasterKey, "투수");
    }

    //S3에서 JSON 파일을 가져오는 공통 로직
    private String fetchFromS3(String key, String type) {
        try {
            log.info("====> [S3] {} 마스터 데이터 로드 시도 (Bucket: {}, Key: {})", type, bucketName, key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
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