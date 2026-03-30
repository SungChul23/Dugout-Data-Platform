package com.dev.dugout.domain.team.controller;


import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.service.TeamRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor

public class TeamRankingController {

    private final TeamRankingService teamRankingService;
    @GetMapping("/ranking")
    public ResponseEntity<List<TeamRankResponseDto>> getDailyRanking() {
        List<TeamRankResponseDto> response = teamRankingService.getLatestTeamRankings();
        return ResponseEntity.ok(response);
    }

}
