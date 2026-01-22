package com.dev.dugout.domain.player.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Null인 필드는 JSON 응답에서 제외
//선수별 상제 분석 응답 전용 DTO
public class PredictionResponseDto {

    private String name;
    private Integer backNumber;
    private String position;

    // --- 타자 전용 지표---
    private BigDecimal predAvg;
    private Integer predHr;
    private BigDecimal predOps;
    private BigDecimal avgDiff;
    private Integer hrDiff;
    private BigDecimal opsDiff;
    private BigDecimal currentAvg;
    private Integer currentHr;
    private BigDecimal currentOps;

    // --- 투수 전용 지표---
    private BigDecimal currentEra;      // 현재 ERA
    private BigDecimal currentWhip;     // 현재 WHIP
    private BigDecimal eraEliteProb;    // 내년 ERA 엘리트 확률
    private BigDecimal whipEliteProb;   // 내년 WHIP 엘리트 확률

    private String aiReport; // 공통 AI 리포트

}
