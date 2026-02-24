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
    private BigDecimal currAvg;
    private BigDecimal predAvg;
    private BigDecimal avgDiff;
    private BigDecimal avgMin;
    private BigDecimal avgMax;

    private BigDecimal currObp;
    private BigDecimal predObp;
    private BigDecimal diffObp;
    private BigDecimal obpMin;
    private BigDecimal obpMax;

    private BigDecimal currSlg;
    private BigDecimal predSlg;
    private BigDecimal diffSlg;
    private BigDecimal slgMin;
    private BigDecimal slgMax;

    private BigDecimal currOps;
    private BigDecimal predOps;
    private BigDecimal opsDiff;
    private BigDecimal opsMin;
    private BigDecimal opsMax;

    private Integer currHr;
    private Integer predHr;
    private Integer hrDiff;
    private Integer hrMin;
    private Integer hrMax;

    // --- 투수 전용 지표 ---
    private BigDecimal probElite;         // 엘리트 확률
    private BigDecimal rolePercentileTop; // 상위 %
    private Integer roleRank;             // 보직 내 순위
    private Integer roleTotal;            // 보직 내 전체 인원

    private BigDecimal era2025;           // 2025년 평균자책점 (ERA)
    private BigDecimal fip2025;           // 2025년 수비무관 투구지표 (FIP)
    private BigDecimal ip2025;            // 2025년 소화 이닝 (IP)
    private BigDecimal whip2025;          // 2025년 이닝당 출루허용률 (WHIP)
    private String role;                  // 투수 보직 (예: SP, RP)

    private String aiReport; // AI 분석 리포트

}
