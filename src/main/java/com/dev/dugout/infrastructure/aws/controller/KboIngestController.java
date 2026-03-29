package com.dev.dugout.infrastructure.aws.controller;

import com.dev.dugout.infrastructure.aws.dto.KboIngestRequest;
import com.dev.dugout.infrastructure.aws.service.KboIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ingest/kbo")
@RequiredArgsConstructor
@Slf4j
public class KboIngestController {

    private final KboIngestService ingestService;

    @PostMapping("/notify")
    public ResponseEntity<String> notifyDataReady(@RequestBody KboIngestRequest request) {
        log.info(">>>> [API] KBO 데이터 입고 신호 수신: {}", request.baseDate());

        // 비동기로 실행하여 외부 시스템(Step Functions 등)에 즉시 응답 반환
        CompletableFuture.runAsync(() -> ingestService.processIngestion(request));

        return ResponseEntity.ok("데이터 입고 요청이 수락되었습니다. 백그라운드에서 공정을 시작합니다.");
    }
}