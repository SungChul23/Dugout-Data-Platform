package com.dev.dugout.infrastructure.aws.controller;


import com.dev.dugout.infrastructure.aws.dto.SurveyRequestDto;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
import com.dev.dugout.infrastructure.aws.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/fanexperience")
@RequiredArgsConstructor
public class TeamRecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/match-team")
    public ResponseEntity<TeamRecommendationResponseDto> matchTeam
            (@RequestBody SurveyRequestDto request){
        return ResponseEntity.ok(recommendationService.getMatchTeam(request));
    }
}
