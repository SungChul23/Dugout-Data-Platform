package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.TeamPerformanceResponseDto;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.service.TeamPerformanceService;
import com.dev.dugout.domain.team.service.TeamRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Team", description = "팀·경기·뉴스·티켓 관련 API")
@ApiResponses({
        @ApiResponse(responseCode = "404", description = "오늘자 팀 데이터 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class TeamRankingAndPerformanceController {

    private final TeamRankingService teamRankingService;
    private final TeamPerformanceService teamPerformanceService;

    @Operation(
            summary = "KBO 팀 순위 조회",
            description = "10개 구단의 최신 팀 순위를 반환합니다. 승률·승차·연속 결과·최근 10경기 성적 포함."
    )
    @ApiResponse(responseCode = "200", description = "팀 순위 반환 성공")
    @GetMapping("/team-ranking")
    public ResponseEntity<List<TeamRankResponseDto>> getDailyRanking() {
        List<TeamRankResponseDto> response = teamRankingService.getAllTeamRankings();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "KBO 팀 성적 조회",
            description = """
                    10개 구단의 최신 팀 타격·투구·수비/주루 통합 성적을 반환합니다.

                    - 타격: AVG, HR, 득점, 안타, RBI, OBP, OPS, RISP, SLG 등 12개 지표
                    - 투구: ERA, W, SO, SV, HLD, WPCT, WHIP, QS 등 12개 지표
                    - 수비/주루: SB, SB성공률, 실책, 수비율, 병살 등 12개 지표
                    """
    )
    @ApiResponse(responseCode = "200", description = "팀 성적 반환 성공")
    @GetMapping("/stats")
    public ResponseEntity<List<TeamPerformanceResponseDto>> getAllTeamStats() {
        return ResponseEntity.ok(teamPerformanceService.getAllTeamPerformances());
    }
}
