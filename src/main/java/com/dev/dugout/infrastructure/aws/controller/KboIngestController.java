package com.dev.dugout.infrastructure.aws.controller;

import com.dev.dugout.infrastructure.aws.dto.GoldenGloveRequestDto;
import com.dev.dugout.infrastructure.aws.dto.KboIngestRequest;
import com.dev.dugout.infrastructure.aws.service.GGDataIngestService;
import com.dev.dugout.infrastructure.aws.service.KBODataIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Tag(name = "AWS", description = "AWS Lambda → Spring 데이터 입고 API (내부 전용) — Bearer 시크릿 키 인증 필요")
@SecurityRequirement(name = "LambdaSecret")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Lambda 시크릿 키 불일치"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
@Slf4j
public class KboIngestController {

    private final KBODataIngestService ingestService;
    private final GGDataIngestService ggDataIngestService;

    @Value("${aws.lambda.secret-key}")
    private String lambdaSecretKey;

    private boolean isInvalidToken(String authHeader) {
        return authHeader == null || !authHeader.equals("Bearer " + lambdaSecretKey);
    }

    @Operation(
            summary = "KBO 일일 데이터 입고 알림",
            description = """
                    AWS Step Functions Lambda가 크롤링 완료 후 Spring 서버에 S3 경로를 전달합니다.
                    서버는 비동기(`CompletableFuture`)로 S3 데이터를 읽어 RDS에 적재합니다.

                    **처리 데이터 유형**: TEAM_RANK, PLAYER_HITTER, PLAYER_PITCHER, TEAM_STATS, GAME_RESULT
                    """
    )
    @RequestBody(
            description = "KBO 데이터 입고 명세서",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "입고 요청 예시",
                            value = """
                                    {
                                      "baseDate": "2026-06-06",
                                      "files": [
                                        { "type": "PLAYER_HITTER", "s3Path": "s3://dugout-data/hitter/2026-06-06.csv" },
                                        { "type": "PLAYER_PITCHER", "s3Path": "s3://dugout-data/pitcher/2026-06-06.csv" },
                                        { "type": "TEAM_RANK",    "s3Path": "s3://dugout-data/team-rank/2026-06-06.csv" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "입고 요청 수락 (백그라운드 처리 시작)")
    @PostMapping("/kbo/notify")
    public ResponseEntity<String> notifyDataReady(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader,
            @org.springframework.web.bind.annotation.RequestBody KboIngestRequest request) {

        if (isInvalidToken(authHeader)) {
            log.warn(">>>> [API] KBO 입고 신호 인증 실패");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        log.info(">>>> [API] KBO 데이터 입고 신호 수신: {}", request.baseDate());

        CompletableFuture.runAsync(() -> ingestService.processIngestion(request));

        return ResponseEntity.ok("데이터 입고 요청이 수락되었습니다. 백그라운드에서 공정을 시작합니다.");
    }

    @Operation(
            summary = "골든글러브 예측 데이터 입고",
            description = """
                    AWS Lambda의 ML 배치 추론 결과(XGBoost 골든글러브 예측)를 RDS에 적재합니다.
                    AI 설명(Bedrock 피드백)이 포함된 포지션별 선수 예측 데이터를 일괄 저장합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "골든글러브 예측 데이터 입고 성공")
    @PostMapping("/ml/gg")
    public ResponseEntity<String> ingestLeaderboard(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader,
            @org.springframework.web.bind.annotation.RequestBody GoldenGloveRequestDto.IngestRequest request) {

        if (isInvalidToken(authHeader)) {
            log.warn(">>>> [API] GG 예측 입고 신호 인증 실패");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        ggDataIngestService.ingestPredictions(request);

        return ResponseEntity.ok("골든글러브 예측 데이터의 입고 요청이 수락되었습니다. " + request.getBaseDate());
    }
}
