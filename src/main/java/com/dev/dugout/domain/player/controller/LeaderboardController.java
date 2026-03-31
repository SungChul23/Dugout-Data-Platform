package com.dev.dugout.domain.player.controller;


import com.dev.dugout.domain.player.dto.LeaderboardDto;
import com.dev.dugout.domain.player.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/hitter")
    public ResponseEntity<List<LeaderboardDto.Response>> getHitterLeaderboard(
            @RequestParam(defaultValue = "NORMAL") String type) {
        return ResponseEntity.ok(leaderboardService.getHitterLeaderboard(type));
    }

    @GetMapping("/pitcher")
    public ResponseEntity<List<LeaderboardDto.Response>> getPitcherLeaderboard(
            @RequestParam(defaultValue = "NORMAL") String type) {
        return ResponseEntity.ok(leaderboardService.getPitcherLeaderboard(type));
    }

}
