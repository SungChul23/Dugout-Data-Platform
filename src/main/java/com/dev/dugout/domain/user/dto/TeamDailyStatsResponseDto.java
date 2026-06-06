package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "응원 팀 일일 타자·투수 전체 성적 응답 DTO")
@Getter
@Builder
public class TeamDailyStatsResponseDto {

    @Schema(description = "구단명", example = "KIA 타이거즈")
    private String teamName;

    @Schema(description = "성적 기준일", example = "2026-06-06")
    private LocalDate baseDate;

    @Schema(description = "팀 타자 전체 성적 목록")
    private List<PlayerStatDto> hitters;

    @Schema(description = "팀 투수 전체 성적 목록")
    private List<PlayerStatDto> pitchers;

    @Schema(description = "선수 개별 성적 DTO")
    @Getter
    @Builder
    public static class PlayerStatDto {

        @Schema(description = "선수 KBO 코드", example = "79047")
        private String playerId;

        @Schema(description = "선수명", example = "김도영")
        private String playerName;

        @Schema(description = "지표명 → 값 맵 (타자: avg/hr/rbi 등 / 투수: era/ip/so 등)")
        private Map<String, Number> stats;
    }
}
