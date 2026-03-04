package com.dev.dugout.infrastructure.aws.controller;


import com.dev.dugout.infrastructure.aws.dto.SurveyRequestDto;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
import com.dev.dugout.infrastructure.aws.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api/v1/fanexperience")
@RequiredArgsConstructor
public class TeamRecommendationController {

    private final RecommendService recommendationService;

    @PostMapping("/match-team")
    public ResponseEntity<List<TeamRecommendationResponseDto>> matchTeam(
            @RequestBody SurveyRequestDto request) {
        // 서비스에서 반환하는 타입이 List<TeamRecommendationResponseDto>로 변경됨
        List<TeamRecommendationResponseDto> recommendations = recommendationService.getMatchTeam(request);
        return ResponseEntity.ok(recommendations);
    }
}
