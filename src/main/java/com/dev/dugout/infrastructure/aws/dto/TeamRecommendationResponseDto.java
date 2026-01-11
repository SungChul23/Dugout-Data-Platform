package com.dev.dugout.infrastructure.aws.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor //모든 필드를 파라미터로 받는  생성자 자동 생성
@NoArgsConstructor //파라미터가 없는 기본 생성자를 자동 생성
public class TeamRecommendationResponseDto {

    private String year;
    private String teamName;     // "두산 베어스"와 같은 풀네임
    private String originalName; // "두산" (DB 저장 명칭)
    private double score;
    private String reason;       // Bedrock용 요약 필드
}
