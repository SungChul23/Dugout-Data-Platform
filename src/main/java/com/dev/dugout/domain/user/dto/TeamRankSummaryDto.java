package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "대시보드용 응원 팀 순위 요약 DTO")
@Getter
@Builder
public class TeamRankSummaryDto {

    @Schema(description = "팀 고유 ID", example = "5")
    private Long teamId;

    @Schema(description = "현재 순위", example = "1")
    private Integer rank;

    @Schema(description = "승-무-패 전적", example = "47-2-25")
    private String wdl;

    @Schema(description = "승률 문자열", example = "0.655")
    private String winRate;
}
