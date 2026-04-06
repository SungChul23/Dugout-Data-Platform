package com.dev.dugout.domain.player.controller;


import com.dev.dugout.domain.player.dto.GoldenGloveResponseDto;
import com.dev.dugout.domain.player.service.GoldenGloveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/gg")
@RequiredArgsConstructor
public class GoldenGloveController {

    private final GoldenGloveService goldenGloveService;

    // 골든글러브 리더보드 조회 API
    // 일반 포지션 TOP 3, 외야수(OF) TOP 6 및 상세 포지션 정보 포함
    @GetMapping("/leaderboard/latest")
    public ResponseEntity<GoldenGloveResponseDto.LeaderboardResponse> getLatestLeaderboard() {

        log.info(">>>> [API] 프론트엔드 최신 골든글러브 리더보드 조회 요청 수신");

        GoldenGloveResponseDto.LeaderboardResponse response = goldenGloveService.getLatestLeaderboard();

        log.info("<<<< [API] 응답 완료: 기준일 [{}]", response.getBaseDate());

        return ResponseEntity.ok(response);
    }
}
