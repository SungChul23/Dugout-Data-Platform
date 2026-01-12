package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dev.dugout.global.config.UserPrincipal; // 패키지 임포트 확인!

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    // 대시보드 진입 시 호출되는 API
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // 현재 로그인한 유저 정보로 대시보드 데이터 조회
        DashboardResponseDto response = dashboardService.getUserDashboard(userPrincipal.getUser());
        return ResponseEntity.ok(response);
    }




}
