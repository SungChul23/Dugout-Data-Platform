package com.dev.dugout.domain.postseason.dto;

import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "가을야구 5강 브라켓 응답 DTO")
@Builder
@Getter
public class PostseasonBracketResponseDto {

    @Schema(description = "현재 순위 1~5위 팀 목록 (순위 오름차순)")
    private List<TeamRankResponseDto> top5Teams;

    @Schema(description = "포스트시즌 라운드별 매치업")
    private List<MatchupDto> matchups;

    @Schema(description = "포스트시즌 매치업 (상위 시드 vs 하위 시드)")
    @Builder
    @Getter
    public static class MatchupDto {
        @Schema(description = "라운드 구분", example = "WILD_CARD")
        private String round;

        @Schema(description = "상위 시드 팀 ID", example = "3")
        private Long higherSeedTeamId;

        @Schema(description = "상위 시드 팀명", example = "KIA 타이거즈")
        private String higherSeedTeamName;

        @Schema(description = "하위 시드 팀 ID (미확정 시 null)", example = "9")
        private Long lowerSeedTeamId;

        @Schema(description = "하위 시드 팀명 (미확정 시 TBD)", example = "NC 다이노스")
        private String lowerSeedTeamName;
    }
}
