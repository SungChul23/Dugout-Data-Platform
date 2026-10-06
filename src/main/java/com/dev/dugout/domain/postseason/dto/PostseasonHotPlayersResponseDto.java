package com.dev.dugout.domain.postseason.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "가을야구 5강 팀 최근 폼(뜨거운 타자/투수) 응답 DTO")
@Builder
@Getter
public class PostseasonHotPlayersResponseDto {

    @Schema(description = "집계 기간(일)", example = "14")
    private Integer days;

    @Schema(description = "집계 시작일 (데이터가 없으면 null)", example = "2026-09-23")
    private LocalDate startDate;

    @Schema(description = "집계 종료일 = 최신 데이터 날짜 (데이터가 없으면 null)", example = "2026-10-06")
    private LocalDate endDate;

    @Schema(description = "뜨거운 타자 (기간 OPS 높은 순)")
    private List<HotBatterDto> hotBatters;

    @Schema(description = "뜨거운 투수 (선발 → 불펜 순, 각 보직 내 기간 ERA 낮은 순)")
    private List<HotPitcherDto> hotPitchers;

    @Schema(description = "기간 타격 성적")
    @Builder
    @Getter
    public static class HotBatterDto {
        private String playerName;
        private Long teamId;
        private String teamName;

        @Schema(description = "기간 타석", example = "48")
        private Integer pa;
        @Schema(description = "기간 타율", example = "0.385")
        private BigDecimal avg;
        @Schema(description = "기간 홈런", example = "4")
        private Integer hr;
        @Schema(description = "기간 타점", example = "11")
        private Integer rbi;
        @Schema(description = "기간 OPS", example = "1.102")
        private BigDecimal ops;

        @Schema(description = "시즌 타율", example = "0.301")
        private BigDecimal seasonAvg;
        @Schema(description = "시즌 OPS", example = "0.845")
        private BigDecimal seasonOps;
        @Schema(description = "기간 OPS - 시즌 OPS (양수면 시즌 평균보다 상승세)", example = "0.257")
        private BigDecimal opsDiff;
    }

    @Schema(description = "기간 투구 성적")
    @Builder
    @Getter
    public static class HotPitcherDto {
        @Schema(description = "기간 내 보직 (STARTER: 선발, RELIEVER: 불펜)", example = "STARTER")
        private String role;
        private String playerName;
        private Long teamId;
        private String teamName;

        @Schema(description = "기간 등판 경기 수", example = "3")
        private Integer games;
        @Schema(description = "기간 이닝 (⅓ = .33, ⅔ = .67)", example = "18.67")
        private BigDecimal ip;
        @Schema(description = "기간 ERA", example = "1.45")
        private BigDecimal era;
        @Schema(description = "기간 WHIP", example = "0.96")
        private BigDecimal whip;
        @Schema(description = "기간 탈삼진", example = "21")
        private Integer so;
        @Schema(description = "기간 승", example = "2")
        private Integer w;
        @Schema(description = "기간 세이브", example = "0")
        private Integer sv;
        @Schema(description = "기간 홀드", example = "0")
        private Integer hld;

        @Schema(description = "시즌 ERA", example = "3.12")
        private BigDecimal seasonEra;
        @Schema(description = "기간 ERA - 시즌 ERA (음수면 시즌 평균보다 상승세)", example = "-1.67")
        private BigDecimal eraDiff;
    }
}
