package com.dev.dugout.domain.player.controller;

import com.dev.dugout.domain.player.dto.PlayerResponseDto;
import com.dev.dugout.domain.player.dto.PredictionResponseDto;
import com.dev.dugout.domain.player.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Player", description = "선수 관련 API — 성적 예측, FA 시장, 골든글러브, 리더보드")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content),
        @ApiResponse(responseCode = "404", description = "선수 또는 예측 데이터 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService analysisService;

    @Operation(
            summary = "구단별 선수 명단 조회",
            description = "구단명과 포지션 타입으로 해당 구단 선수 목록을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "선수 명단 반환 성공")
    @GetMapping("/players")
    public ResponseEntity<List<PlayerResponseDto>> getRoster(
            @Parameter(description = "구단명 (한글 풀네임)", example = "KIA 타이거즈", required = true)
            @RequestParam("team") String teamName,
            @Parameter(description = "포지션 타입 (hitter: 타자 / pitcher: 투수)", example = "hitter")
            @RequestParam(value = "type", defaultValue = "hitter") String type) {
        List<PlayerResponseDto> roster = analysisService.getRoster(teamName, type);
        return ResponseEntity.ok(roster);
    }

    @Operation(
            summary = "선수 성적 예측 및 AI 리포트 조회",
            description = """
                    XGBoost 앙상블 모델의 시즌 종료 시점 성적 예측과 Amazon Bedrock AI 분석 리포트를 반환합니다.

                    **타자**: 타율(AVG), OBP, SLG, OPS, 홈런(HR) 예측값·현재값·차이·범위
                    **투수**: 엘리트 등극 확률, 보직 내 순위, 2025 시즌 ERA·FIP·IP·WHIP

                    SHAP 기반 설명 데이터는 Java 힙 메모리에서 조회 후 Bedrock에 전달됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "예측 및 AI 리포트 반환 성공")
    @GetMapping("/prediction")
    public ResponseEntity<PredictionResponseDto> getPrediction(
            @Parameter(description = "선수 고유 ID (KBO 선수 코드)", example = "79047", required = true)
            @RequestParam("playerId") Long playerId) {
        PredictionResponseDto prediction = analysisService.getAnalysis(playerId);
        return ResponseEntity.ok(prediction);
    }
}
