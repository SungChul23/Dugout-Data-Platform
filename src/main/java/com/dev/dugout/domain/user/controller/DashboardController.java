package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.DashboardRequestDto;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.dto.TeamDailyStatsResponseDto;
import com.dev.dugout.domain.user.service.DashboardService;
import com.dev.dugout.global.config.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "회원 인증 및 개인화 대시보드 API")
@SecurityRequirement(name = "CookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요 (로그인 후 이용)", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "개인화 대시보드 조회",
            description = "로그인한 사용자의 응원 팀 뉴스, 경기 일정, 즐겨찾기 선수 예측 성적을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "대시보드 데이터 반환 성공")
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DashboardResponseDto response =
                dashboardService.getUserDashboard(userPrincipal.getUser());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "응원 선수 즐겨찾기 추가",
            description = "대시보드에 표시할 응원 선수를 추가합니다. 슬롯은 최대 3개이며, 초과 시 400을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "선수 추가 성공")
    @ApiResponse(responseCode = "400", description = "슬롯이 가득 찼거나 이미 등록된 선수", content = @Content)
    @PostMapping("/player")
    public ResponseEntity<?> addPlayer(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody DashboardRequestDto request) {
        try {
            dashboardService.addPlayer(userPrincipal.getUser(), request.getPlayerId());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "응원 선수 즐겨찾기 삭제",
            description = "슬롯 번호로 대시보드의 응원 선수를 제거합니다."
    )
    @ApiResponse(responseCode = "200", description = "선수 삭제 성공")
    @DeleteMapping("/player")
    public ResponseEntity<Void> removePlayer(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "삭제할 슬롯 번호 (1~3)", example = "1", required = true)
            @RequestParam int slotNumber) {
        dashboardService.removePlayer(userPrincipal.getUser(), slotNumber);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "응원 팀 일일 타자·투수 성적 조회",
            description = "로그인한 사용자의 응원 팀 전체 선수의 당일 성적 테이블 데이터를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "팀 일일 성적 반환 성공")
    @GetMapping("/team-daily-stats")
    public ResponseEntity<TeamDailyStatsResponseDto> getTeamDailyStats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TeamDailyStatsResponseDto response = dashboardService.getTeamDailyStats(userPrincipal.getUser());
        return ResponseEntity.ok(response);
    }
}
