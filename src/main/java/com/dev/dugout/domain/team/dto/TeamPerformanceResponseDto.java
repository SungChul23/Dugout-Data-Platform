package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "KBO 팀 통합 성적 응답 DTO — 타격·투구·수비/주루 36개 지표")
@Getter
@Builder
public class TeamPerformanceResponseDto {

    @Schema(description = "팀 고유 ID", example = "5")
    private Long teamId;

    @Schema(description = "팀명", example = "KIA 타이거즈")
    private String teamName;

    @Schema(description = "성적 기준일", example = "2026-06-06")
    private LocalDate baseDate;

    // 팀 타자 지표
    @Schema(description = "팀 타율", example = "0.290")
    private BigDecimal avg;

    @Schema(description = "팀 홈런", example = "105")
    private Integer hr;

    @Schema(description = "팀 득점", example = "412")
    private Integer runs;

    @Schema(description = "팀 안타", example = "801")
    private Integer hits;

    @Schema(description = "팀 타점", example = "395")
    private Integer rbi;

    @Schema(description = "팀 출루율", example = "0.368")
    private BigDecimal obp;

    @Schema(description = "팀 OPS", example = "0.812")
    private BigDecimal ops;

    @Schema(description = "팀 득점권 타율", example = "0.278")
    private BigDecimal risp;

    @Schema(description = "팀 장타율", example = "0.444")
    private BigDecimal slg;

    @Schema(description = "팀 대타 타율", example = "0.245")
    private BigDecimal phBa;

    @Schema(description = "팀 멀티히트 경기 수", example = "45")
    private Integer multiHit;

    @Schema(description = "팀 총루타", example = "1189")
    private Integer totalBases;

    // 팀 투수 지표
    @Schema(description = "팀 방어율", example = "3.45")
    private BigDecimal era;

    @Schema(description = "팀 승리 수", example = "47")
    private Integer wins;

    @Schema(description = "팀 탈삼진", example = "612")
    private Integer so;

    @Schema(description = "팀 세이브", example = "21")
    private Integer sv;

    @Schema(description = "팀 홀드", example = "38")
    private Integer hld;

    @Schema(description = "팀 승률", example = "0.655")
    private BigDecimal wpct;

    @Schema(description = "팀 WHIP", example = "1.23")
    private BigDecimal whip;

    @Schema(description = "팀 퀄리티스타트(QS)", example = "28")
    private Integer qs;

    @Schema(description = "팀 피안타율", example = "0.245")
    private BigDecimal oppAvg;

    @Schema(description = "팀 블론세이브", example = "8")
    private Integer bsv;

    @Schema(description = "팀 총 투구수", example = "12450")
    private Integer np;

    @Schema(description = "팀 피홈런", example = "65")
    private Integer hrAllowed;

    // 팀 수비/주루 지표
    @Schema(description = "팀 도루 성공 수", example = "72")
    private Integer sb;

    @Schema(description = "팀 도루 성공률", example = "0.720")
    private BigDecimal sbRate;

    @Schema(description = "팀 실책 수", example = "38")
    private Integer error;

    @Schema(description = "팀 수비율", example = "0.981")
    private BigDecimal fpct;

    @Schema(description = "팀 병살 처리 수", example = "55")
    private Integer dp;

    @Schema(description = "팀 도루 저지율", example = "0.280")
    private BigDecimal csRate;

    @Schema(description = "팀 출루 허용 수", example = "520")
    private Integer oob;

    @Schema(description = "팀 도루 시도 허용 수", example = "58")
    private Integer sba;

    @Schema(description = "팀 견제 성공 (주루)", example = "5")
    private Integer pkoR;

    @Schema(description = "팀 견제 성공 (수비)", example = "3")
    private Integer pkoD;

    @Schema(description = "팀 도루 저지 수", example = "16")
    private Integer cs;

    @Schema(description = "팀 도루 허용 수", example = "42")
    private Integer sbAllowed;
}
