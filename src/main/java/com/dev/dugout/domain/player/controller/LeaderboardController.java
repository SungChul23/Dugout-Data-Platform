package com.dev.dugout.domain.player.controller;

import com.dev.dugout.domain.player.dto.LeaderboardDto;
import com.dev.dugout.domain.player.service.LeaderboardService;
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
        @ApiResponse(responseCode = "400", description = "잘못된 type 파라미터", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @Operation(
            summary = "타자 지표 리더보드 조회",
            description = """
                    타자 지표별 TOP 5 선수를 반환합니다. 결과는 10분간 캐시됩니다.

                    **type 값별 제공 지표**
                    - `NORMAL`: 타율(AVG), 홈런(HR), 안타(H), 타점(RBI), 출루율(OBP)
                    - `ADVANCED`: OPS, 장타율(SLG), 득점권타율(RISP), 장타(XBH), 총루타(TB)
                    """
    )
    @ApiResponse(responseCode = "200", description = "타자 리더보드 반환 성공")
    @GetMapping("/hitter")
    public ResponseEntity<List<LeaderboardDto.Response>> getHitterLeaderboard(
            @Parameter(description = "지표 유형 (NORMAL: 기본 5개 / ADVANCED: 심화 5개)", example = "NORMAL")
            @RequestParam(defaultValue = "NORMAL") String type) {
        return ResponseEntity.ok(leaderboardService.getHitterLeaderboard(type));
    }

    @Operation(
            summary = "투수 지표 리더보드 조회",
            description = """
                    투수 지표별 TOP 5 선수를 반환합니다. 결과는 10분간 캐시됩니다.

                    **type 값별 제공 지표**
                    - `NORMAL`: 방어율(ERA), 탈삼진(SO), 세이브(SV), 홀드(HLD), WHIP
                    - `ADVANCED`: QS, 피안타율(OppAVG), 블론세이브(BSV), 투구수(NP), 이닝(IP)
                    """
    )
    @ApiResponse(responseCode = "200", description = "투수 리더보드 반환 성공")
    @GetMapping("/pitcher")
    public ResponseEntity<List<LeaderboardDto.Response>> getPitcherLeaderboard(
            @Parameter(description = "지표 유형 (NORMAL: 기본 5개 / ADVANCED: 심화 5개)", example = "NORMAL")
            @RequestParam(defaultValue = "NORMAL") String type) {
        return ResponseEntity.ok(leaderboardService.getPitcherLeaderboard(type));
    }
}
