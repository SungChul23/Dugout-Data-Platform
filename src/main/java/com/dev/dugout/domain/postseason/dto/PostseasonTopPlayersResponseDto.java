package com.dev.dugout.domain.postseason.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "팀별 주요 선수 TOP3 응답 DTO")
@Builder
@Getter
public class PostseasonTopPlayersResponseDto {

    @Schema(description = "팀 ID", example = "5")
    private Long teamId;

    @Schema(description = "팀명", example = "KIA 타이거즈")
    private String teamName;

    @Schema(description = "타자 TOP3 (OPS 기준)")
    private List<BatterStatDto> topBatters;

    @Schema(description = "투수 TOP3 (ERA 기준)")
    private List<PitcherStatDto> topPitchers;

    @Schema(description = "타자 주요 스탯")
    @Builder
    @Getter
    public static class BatterStatDto {
        private String playerName;
        private BigDecimal avg;
        private Integer hr;
        private BigDecimal ops;
    }

    @Schema(description = "투수 주요 스탯")
    @Builder
    @Getter
    public static class PitcherStatDto {
        private String playerName;
        private BigDecimal era;
        private Integer w;
        private Integer so;
    }
}
