package com.dev.dugout.domain.player.controller;

import com.dev.dugout.domain.player.dto.GoldenGloveResponseDto;
import com.dev.dugout.domain.player.service.GoldenGloveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Player", description = "선수 관련 API — 성적 예측, FA 시장, 골든글러브, 리더보드")
@ApiResponses({
        @ApiResponse(responseCode = "404", description = "골든글러브 예측 데이터 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
@Slf4j
@RestController
@RequestMapping("/api/v1/gg")
@RequiredArgsConstructor
public class GoldenGloveController {

    private final GoldenGloveService goldenGloveService;

    @Operation(
            summary = "골든글러브 예측 리더보드 조회",
            description = """
                    XGBoost 모델 기반의 최신 골든글러브 수상 예측 결과를 포지션별로 반환합니다.

                    - 일반 포지션(C, 1B, 2B, 3B, SS, DH, SP, RP): TOP 3
                    - 외야수(OF): TOP 6 (세부 포지션 LF/CF/RF 포함)
                    - SHAP 기반 긍정/부정 기여 지표 및 AI 설명 포함
                    """
    )
    @ApiResponse(responseCode = "200", description = "골든글러브 리더보드 반환 성공")
    @GetMapping("/leaderboard/latest")
    public ResponseEntity<GoldenGloveResponseDto.LeaderboardResponse> getLatestLeaderboard() {

        log.info(">>>> [API] 프론트엔드 최신 골든글러브 리더보드 조회 요청 수신");

        GoldenGloveResponseDto.LeaderboardResponse response = goldenGloveService.getLatestLeaderboard();

        log.info("<<<< [API] 응답 완료: 기준일 [{}]", response.getBaseDate());

        return ResponseEntity.ok(response);
    }
}
