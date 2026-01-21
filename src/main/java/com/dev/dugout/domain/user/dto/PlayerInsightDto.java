package com.dev.dugout.domain.user.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerInsightDto {

    private Integer slotNumber;
    private Long playerId;
    private String name;
    private String position;


    // 2026년 예상 핵심 지표
    private BigDecimal predictedAvg;   // 예상 타율 (예: 0.312)
    private Integer predictedHr;   // 예상 홈런 (예: 25)
    private BigDecimal predictedOps;   // 예상 OPS (예: 0.895)

    // 2025년 대비 변화폭
    private BigDecimal avgDiff;        // 타율 차이
    private Integer hrDiff;        // 홈런 차이
    private BigDecimal opsDiff;        // OPS 차이

    // Bedrock이 생성한 요약 문구 또는 상세 인사이트 JSON
    private String insightSummary;

    // 슬롯이 비어있는지 여부
    @Builder.Default
    private Boolean isEmpty = true;
}
