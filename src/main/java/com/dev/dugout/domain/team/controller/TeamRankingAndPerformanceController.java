package com.dev.dugout.domain.team.controller;


import com.dev.dugout.domain.team.dto.TeamPerformanceResponseDto;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.service.TeamPerformanceService;
import com.dev.dugout.domain.team.service.TeamRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor

public class TeamRankingAndPerformanceController {

    private final TeamRankingService teamRankingService;
    private final TeamPerformanceService teamPerformanceService;

    // 팀 성적
    @GetMapping("/team-ranking")
    public ResponseEntity<List<TeamRankResponseDto>> getDailyRanking() {
        List<TeamRankResponseDto> response = teamRankingService.getAllTeamRankings();
        return ResponseEntity.ok(response);
    }

    // 기록탭 - 팀 성적
    @GetMapping("/stats")
    public ResponseEntity<List<TeamPerformanceResponseDto>> getAllTeamStats() {
        // baseDate가 포함된 10개 팀의 통합 퍼포먼스 데이터 반환
        return ResponseEntity.ok(teamPerformanceService.getAllTeamPerformances());
    }

}
