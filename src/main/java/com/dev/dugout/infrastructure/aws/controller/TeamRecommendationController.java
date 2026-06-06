package com.dev.dugout.infrastructure.aws.controller;

import com.dev.dugout.infrastructure.aws.dto.SurveyRequestDto;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponseDto;
import com.dev.dugout.infrastructure.aws.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "AWS", description = "AWS Lambda → Spring 데이터 입고 API (내부 전용) — Bearer 시크릿 키 인증 필요")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 Athena 쿼리 실패")
})
@Controller
@RequestMapping("/api/v1/fanexperience")
@RequiredArgsConstructor
public class TeamRecommendationController {

    private final RecommendService recommendationService;

    @Operation(
            summary = "취향 기반 KBO 팀 추천",
            description = """
                    사용자가 선택한 6가지 야구 취향 가중치와 야구 입문 연도를 기반으로 최적의 KBO 구단 TOP 3를 추천합니다.

                    **가중치 항목 (q1~q6, 값: 1~5)**
                    - q1: 홈런(장타력) 선호도
                    - q2: 팀 타율(정교함) 선호도
                    - q3: 선발 방어율(투수력) 선호도
                    - q4: 불펜(세이브+홀드) 선호도
                    - q5: OPS(공격 효율) 선호도
                    - q6: 팀 승률(강팀) 선호도

                    Athena에서 사용자 가중치를 실시간 적용하여 역대 팀 성적 점수를 산출하고,
                    Amazon Bedrock이 상위 3팀에 대한 통합 분석 리포트를 생성합니다.
                    """
    )
    @RequestBody(
            description = "팀 추천 설문 요청",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "홈런·승률 중시 팬 예시",
                            value = """
                                    {
                                      "startYear": 2018,
                                      "preferences": {
                                        "q1": 5,
                                        "q2": 2,
                                        "q3": 3,
                                        "q4": 3,
                                        "q5": 4,
                                        "q6": 5
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "팀 추천 결과 반환 성공 (TOP 3 구단 + AI 분석 리포트)")
    @PostMapping("/match-team")
    public ResponseEntity<List<TeamRecommendationResponseDto>> matchTeam(
            @org.springframework.web.bind.annotation.RequestBody SurveyRequestDto request) {
        List<TeamRecommendationResponseDto> recommendations = recommendationService.getMatchTeam(request);
        return ResponseEntity.ok(recommendations);
    }
}
