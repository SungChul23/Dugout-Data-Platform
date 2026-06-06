package com.dev.dugout.domain.player.controller;

import com.dev.dugout.domain.player.dto.FaDetailResponseDto;
import com.dev.dugout.domain.player.dto.FaListResponseDto;
import com.dev.dugout.domain.player.service.FaMarketService;
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
        @ApiResponse(responseCode = "404", description = "선수 또는 데이터를 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/fa-market")
@RequiredArgsConstructor
public class FaMarketController {

    private final FaMarketService faMarketService;

    @Operation(
            summary = "FA 선수 목록 조회",
            description = "특정 연도 및 구단의 FA 선수 명단을 반환합니다. `faStatus`가 '잔류'인 선수는 제외됩니다."
    )
    @ApiResponse(responseCode = "200", description = "FA 선수 목록 반환 성공")
    @GetMapping("/list")
    public ResponseEntity<List<FaListResponseDto>> getList(
            @Parameter(description = "조회 연도 (FA 자격 취득 연도)", example = "2026", required = true)
            @RequestParam("year") Integer year,
            @Parameter(description = "구단명 (전체 조회 시 'ALL')", example = "KIA 타이거즈", required = true)
            @RequestParam("team") String team) {
        return ResponseEntity.ok(faMarketService.getFaList(year, team));
    }

    @Operation(
            summary = "FA 선수 상세 조회",
            description = "선수 ID로 FA 등급(A/B/C), 공격·수비·기여도 점수 및 Amazon Bedrock AI 분석 피드백을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "FA 선수 상세 정보 반환 성공")
    @GetMapping("/detail")
    public ResponseEntity<FaDetailResponseDto> getDetail(
            @Parameter(description = "선수 고유 ID (KBO 선수 코드)", example = "79047", required = true)
            @RequestParam("playerId") Long playerId) {
        return ResponseEntity.ok(faMarketService.getFaDetail(playerId));
    }
}
