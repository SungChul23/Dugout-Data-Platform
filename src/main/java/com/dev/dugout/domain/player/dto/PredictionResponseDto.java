package com.dev.dugout.domain.player.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Schema(description = "선수 성적 예측 및 AI 리포트 응답 DTO — 타자·투수 공용 (해당 없는 필드는 null 제외)")
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictionResponseDto {

    @Schema(description = "선수명", example = "김도영")
    private String name;

    @Schema(description = "등번호", example = "7")
    private Integer backNumber;

    @Schema(description = "포지션", example = "SS")
    private String position;

    // --- 타자 전용 ---
    @Schema(description = "[타자] 현재 타율", example = "0.358")
    private BigDecimal currAvg;

    @Schema(description = "[타자] 예측 시즌 종료 타율", example = "0.341")
    private BigDecimal predAvg;

    @Schema(description = "[타자] 타율 변화량 (예측 - 현재)", example = "-0.017")
    private BigDecimal avgDiff;

    @Schema(description = "[타자] 예측 타율 하한", example = "0.318")
    private BigDecimal avgMin;

    @Schema(description = "[타자] 예측 타율 상한", example = "0.364")
    private BigDecimal avgMax;

    @Schema(description = "[타자] 현재 출루율(OBP)", example = "0.431")
    private BigDecimal currObp;

    @Schema(description = "[타자] 예측 시즌 종료 OBP", example = "0.412")
    private BigDecimal predObp;

    @Schema(description = "[타자] OBP 변화량", example = "-0.019")
    private BigDecimal diffObp;

    @Schema(description = "[타자] 예측 OBP 하한", example = "0.389")
    private BigDecimal obpMin;

    @Schema(description = "[타자] 예측 OBP 상한", example = "0.435")
    private BigDecimal obpMax;

    @Schema(description = "[타자] 현재 장타율(SLG)", example = "0.612")
    private BigDecimal currSlg;

    @Schema(description = "[타자] 예측 시즌 종료 SLG", example = "0.589")
    private BigDecimal predSlg;

    @Schema(description = "[타자] SLG 변화량", example = "-0.023")
    private BigDecimal diffSlg;

    @Schema(description = "[타자] 예측 SLG 하한", example = "0.551")
    private BigDecimal slgMin;

    @Schema(description = "[타자] 예측 SLG 상한", example = "0.627")
    private BigDecimal slgMax;

    @Schema(description = "[타자] 현재 OPS", example = "1.043")
    private BigDecimal currOps;

    @Schema(description = "[타자] 예측 시즌 종료 OPS", example = "1.001")
    private BigDecimal predOps;

    @Schema(description = "[타자] OPS 변화량", example = "-0.042")
    private BigDecimal opsDiff;

    @Schema(description = "[타자] 예측 OPS 하한", example = "0.940")
    private BigDecimal opsMin;

    @Schema(description = "[타자] 예측 OPS 상한", example = "1.062")
    private BigDecimal opsMax;

    @Schema(description = "[타자] 현재 홈런 수", example = "24")
    private Integer currHr;

    @Schema(description = "[타자] 예측 시즌 종료 홈런 수", example = "35")
    private Integer predHr;

    @Schema(description = "[타자] 홈런 변화량 (예측 - 현재)", example = "11")
    private Integer hrDiff;

    @Schema(description = "[타자] 예측 홈런 하한", example = "29")
    private Integer hrMin;

    @Schema(description = "[타자] 예측 홈런 상한", example = "41")
    private Integer hrMax;

    // --- 투수 전용 ---
    @Schema(description = "[투수] 다음 시즌 엘리트 등극 확률 (0~1)", example = "0.823")
    private BigDecimal probElite;

    @Schema(description = "[투수] 보직(선발/불펜) 내 상위 백분율", example = "12.5")
    private BigDecimal rolePercentileTop;

    @Schema(description = "[투수] 보직 내 순위", example = "3")
    private Integer roleRank;

    @Schema(description = "[투수] 보직 내 전체 인원 수", example = "24")
    private Integer roleTotal;

    @Schema(description = "[투수] 2025 시즌 평균자책점(ERA)", example = "2.89")
    private BigDecimal era2025;

    @Schema(description = "[투수] 2025 시즌 수비무관 투구지표(FIP)", example = "3.12")
    private BigDecimal fip2025;

    @Schema(description = "[투수] 2025 시즌 소화 이닝(IP)", example = "178.2")
    private BigDecimal ip2025;

    @Schema(description = "[투수] 2025 시즌 이닝당 출루허용률(WHIP)", example = "1.05")
    private BigDecimal whip2025;

    @Schema(description = "[투수] 투수 보직 (SP: 선발, RP: 불펜)", example = "SP")
    private String role;

    @Schema(description = "Amazon Bedrock AI 분석 리포트 (SHAP 기반 자연어 설명)")
    private String aiReport;
}
