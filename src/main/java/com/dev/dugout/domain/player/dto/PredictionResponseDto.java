package com.dev.dugout.domain.player.dto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
//선수별 상제 분석 응답 전용 DTO
public class PredictionResponseDto {

    private String name;
    private Integer backNumber;
    private String position;

    // 2026 예측치
    private BigDecimal predAvg;
    private Integer predHr;
    private BigDecimal predOps;

    // 변화폭 (Diff)
    private BigDecimal avgDiff;
    private Integer hrDiff;
    private BigDecimal opsDiff;

    // 2025 현재치 (예측치 - 변화폭으로 계산)
    private BigDecimal currentAvg;
    private Integer currentHr;
    private BigDecimal currentOps;

    private String aiReport; // Bedrock으로 리포트 생성 결과
}
