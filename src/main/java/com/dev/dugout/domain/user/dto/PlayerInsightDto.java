package com.dev.dugout.domain.user.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerInsightDto {

    private Integer slotNumber;
    private Long playerId;
    private String name;
    private String position;
    private String teamCode; // 추가: 선수의 소속 팀 코드 (예: "SS", "HH" 등)
    private boolean isEmpty;

    // --- 타자용 지표 ---
    private BigDecimal predictedAvg;
    private BigDecimal predictedOps;
    private Integer predictedHr;
    private BigDecimal avgDiff;
    private BigDecimal opsDiff;
    private Integer hrDiff;

    // --- 투수용 지표 ---
    private BigDecimal probElite;
    private BigDecimal rolePercentileTop;
    private Integer roleRank;
    private Integer roleTotal;
}
