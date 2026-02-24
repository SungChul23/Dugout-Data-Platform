package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.DashboardRequestDto;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.dev.dugout.global.config.UserPrincipal;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    // 대시보드 진입 시 호출되는 API
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 현재 로그인한 유저 정보로 대시보드 데이터 조회
        DashboardResponseDto response =
                dashboardService.getUserDashboard(userPrincipal.getUser());
        return ResponseEntity.ok(response);
    }

    // 선수추가
    @PostMapping("/player")
    public ResponseEntity<?> addPlayer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody DashboardRequestDto request) {
        try {
            dashboardService.addPlayer(userPrincipal.getUser(), request.getPlayerId());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // "가득 찼습니다" 메시지를 프론트로 전달
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 특정 슬롯 삭제
    @DeleteMapping("/player")
    public ResponseEntity<Void> removePlayer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam int slotNumber) {
        dashboardService.removePlayer(userPrincipal.getUser(), slotNumber);
        return ResponseEntity.ok().build();
    }
}
