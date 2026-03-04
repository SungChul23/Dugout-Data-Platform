package com.dev.dugout.infrastructure.aws.dto;

import lombok.*;

@Getter
@Setter // reason 필드 설정을 위해 Setter 추가
@Builder
@AllArgsConstructor //모든 필드를 파라미터로 받는  생성자 자동 생성
@NoArgsConstructor //파라미터가 없는 기본 생성자를 자동 생성
public class TeamRecommendationResponseDto {

    private String year;         // 예: "2024"
    private String teamName;     // 예: "KIA 타이거즈" (풀네임)
    private String originalName; // 예: "KIA" (DB 저장 명칭)
    private double score;        // 계산된 total_score
    private String reason;       // Bedrock이 생성한 전체 분석 리포트
    private String statsSummary; //AI 분석용 및 프런트엔드 간이 전시용 스탯 요약
}
