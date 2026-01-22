package com.dev.dugout.domain.player.controller;

import com.dev.dugout.domain.player.dto.PlayerResponseDto;
import com.dev.dugout.domain.player.dto.PredictionResponseDto;
import com.dev.dugout.domain.player.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PredictionController {
    private final PredictionService analysisService;


    //구단별 선수 명단 조회
    @GetMapping("/players")
    public ResponseEntity<List<PlayerResponseDto>> getRoster(
            @RequestParam("team") String teamName,
            @RequestParam(value = "type", defaultValue = "hitter") String type) {
        List<PlayerResponseDto> roster = analysisService.getRoster(teamName, type);
        return ResponseEntity.ok(roster);
    }

    //선수별 상세 예측 및 AI 리포트 조회 -> 없다면 베드락 실행
    @GetMapping("/prediction")
    public ResponseEntity<PredictionResponseDto> getPrediction(@RequestParam("playerId") Long playerId) {
        PredictionResponseDto prediction = analysisService.getAnalysis(playerId);
        return ResponseEntity.ok(prediction);
    }
}
