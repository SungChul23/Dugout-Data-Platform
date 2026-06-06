package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "KBO 팀 순위 응답 DTO")
@Builder
@Getter
public class TeamRankResponseDto {

    @Schema(description = "순위 기준일", example = "2026-06-06")
    private LocalDate rankingDate;

    @Schema(description = "팀 고유 ID", example = "5")
    private Long teamId;

    @Schema(description = "팀명", example = "KIA 타이거즈")
    private String teamName;

    @Schema(description = "원정 전적 (승-무-패)", example = "20-1-15")
    private String awayRecord;

    @Schema(description = "무승부 수", example = "2")
    private Integer draws;

    @Schema(description = "1위와의 게임차", example = "3.5")
    private BigDecimal gamesBehind;

    @Schema(description = "홈 전적 (승-무-패)", example = "25-1-10")
    private String homeRecord;

    @Schema(description = "패배 수", example = "25")
    private Integer losses;

    @Schema(description = "현재 순위", example = "1")
    private Integer teamRank;

    @Schema(description = "최근 10경기 성적 (승-무-패)", example = "7-0-3")
    private String recent10games;

    @Schema(description = "연속 결과 (연승/연패)", example = "3연승")
    private String streak;

    @Schema(description = "총 경기 수", example = "72")
    private Integer totalGames;

    @Schema(description = "승률", example = "0.655")
    private BigDecimal winRate;

    @Schema(description = "승리 수", example = "47")
    private Integer wins;

    @Schema(description = "치른 경기 수", example = "72")
    private Integer gamesPlayed;

    @Schema(description = "최근 10경기 결과 (승-무-패)", example = "7-0-3")
    private String last10games;
}
