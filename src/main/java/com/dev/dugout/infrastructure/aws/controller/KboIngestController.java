package com.dev.dugout.infrastructure.aws.controller;

import com.dev.dugout.infrastructure.aws.dto.GoldenGloveRequestDto;
import com.dev.dugout.infrastructure.aws.dto.KboIngestRequest;
import com.dev.dugout.infrastructure.aws.service.GGDataIngestService;
import com.dev.dugout.infrastructure.aws.service.KBODataIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ingest") // 공통 경로로 살짝 다듬었습니다.
@RequiredArgsConstructor
@Slf4j
public class KboIngestController {

    private final KBODataIngestService ingestService;
    private final GGDataIngestService ggDataIngestService;

    // 환경 변수 또는 application.yml에서 주입받는 시크릿 키
    @Value("${aws.lambda.secret-key}")
    private String lambdaSecretKey;

    // 공통 토큰 검증 메서드
    private boolean isInvalidToken(String authHeader) {
        return authHeader == null || !authHeader.equals("Bearer " + lambdaSecretKey);
    }

    // 선수데이터, 경기 결과, 팀 순위 데이터 get
    @PostMapping("/kbo/notify")
    public ResponseEntity<String> notifyDataReady(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody KboIngestRequest request) {

        if (isInvalidToken(authHeader)) {
            log.warn(">>>> [API] KBO 입고 신호 인증 실패");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        log.info(">>>> [API] KBO 데이터 입고 신호 수신: {}", request.baseDate());

        CompletableFuture.runAsync(() -> ingestService.processIngestion(request));

        return ResponseEntity.ok("데이터 입고 요청이 수락되었습니다. 백그라운드에서 공정을 시작합니다.");
    }

    // 골든 글러브 예측 데이터 get (배치 추론)
    @PostMapping("/ml/gg")
    public ResponseEntity<String> ingestLeaderboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody GoldenGloveRequestDto.IngestRequest request) {

        if (isInvalidToken(authHeader)) {
            log.warn(">>>> [API] GG 예측 입고 신호 인증 실패");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        ggDataIngestService.ingestPredictions(request);

        return ResponseEntity.ok("골든글러브 예측 데이터의 입고 요청이 수락되었습니다. " + request.getBaseDate());
    }
}