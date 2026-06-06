package com.dev.dugout.domain.player.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Schema(description = "FA 선수 상세 응답 DTO — 등급·점수·AI 피드백 포함")
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaDetailResponseDto {

    @Schema(description = "선수 고유 ID", example = "79047")
    private Long playerId;

    @Schema(description = "선수명", example = "김도영")
    private String playerName;

    @Schema(description = "주 포지션 코드", example = "3B")
    private String subPositionType;

    @Schema(description = "선수 나이", example = "24")
    private Integer age;

    @Schema(description = "선수 소개", example = "KIA의 핵심 유격수. 공격과 수비를 겸비한 차세대 에이스.")
    private String playerIntro;

    @Schema(description = "ML 모델 기반 FA 등급 (A / B / C)", example = "A")
    private String grade;

    @Schema(description = "현재 연봉 또는 계약 정보", example = "5억원")
    private String currentSalary;

    @Schema(description = "Amazon Bedrock AI 분석 피드백 (SHAP 기반 자연어 설명)")
    private String aiFeedback;

    @Schema(description = "FA 상태", example = "미정")
    private String faStatus;

    @Schema(description = "투수 구위 점수 (0~100, 투수 전용)", example = "82.5")
    private BigDecimal statPitching;

    @Schema(description = "투수 안정성 점수 (0~100, 투수 전용)", example = "76.3")
    private BigDecimal statStability;

    @Schema(description = "타자 공격 점수 (0~100, 타자 전용)", example = "91.2")
    private BigDecimal statOffense;

    @Schema(description = "타자 수비 점수 (0~100, 타자 전용)", example = "78.0")
    private BigDecimal statDefense;

    @Schema(description = "공통 기여도 점수 (0~100)", example = "88.7")
    private BigDecimal statContribution;
}
