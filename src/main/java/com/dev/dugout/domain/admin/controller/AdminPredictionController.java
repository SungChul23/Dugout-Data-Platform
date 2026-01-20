package com.dev.dugout.domain.admin.controller;

import com.dev.dugout.domain.admin.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/predictions")
@RequiredArgsConstructor
public class AdminPredictionController {
    private final PredictionService predictionService;

    // POST 대신 GetMapping을 사용하면 브라우저 주소창에서 바로 실행 가능합니다.
    @GetMapping("/init")
    public ResponseEntity<String> initializePredictions() {
        try {
            predictionService.initPredictionsFromCsv();
            return ResponseEntity.ok("<h1> 성공 </h1><p>2026 시즌 예측 데이터가 정상적으로 적재/업데이트되었습니다.</p>");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<h1> 실패 </h1><p>에러 발생: " + e.getMessage() + "</p>");
        }
    }
}
