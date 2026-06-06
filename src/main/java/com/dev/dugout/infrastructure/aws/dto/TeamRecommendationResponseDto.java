package com.dev.dugout.infrastructure.aws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "KBO 팀 추천 응답 DTO")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamRecommendationResponseDto {

    @Schema(description = "추천 근거가 된 대표 연도", example = "2024")
    private String year;

    @Schema(description = "구단 풀네임", example = "KIA 타이거즈")
    private String teamName;

    @Schema(description = "DB 저장 구단 약칭", example = "KIA")
    private String originalName;

    @Schema(description = "사용자 가중치 적용 총점", example = "4.823")
    private double score;

    @Schema(description = "Amazon Bedrock AI 통합 분석 리포트 (3개 팀 공통 반환)")
    private String reason;

    @Schema(description = "AI 분석용 및 프런트 표시용 스탯 요약", example = "홈런 233, 타율 0.290, ERA 3.45, OPS 0.812, 세이브 42, 홀드 68")
    private String statsSummary;
}
