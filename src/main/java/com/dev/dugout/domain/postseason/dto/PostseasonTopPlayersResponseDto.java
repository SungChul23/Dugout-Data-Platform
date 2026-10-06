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

    @Schema(description = "타자 3명 (규정타석 충족, 타율/홈런/OPS 부문별 1명씩)")
    private List<BatterStatDto> topBatters;

    @Schema(description = "투수 3명 (선발 2명 + 불펜 1명, ERA 낮은 순)")
    private List<PitcherStatDto> topPitchers;

    @Schema(description = "타자 주요 스탯")
    @Builder
    @Getter
    public static class BatterStatDto {
        @Schema(description = "선정 부문 (AVG: 타율, HR: 홈런, OPS)", example = "AVG")
        private String category;
        private String playerName;
        private BigDecimal avg;
        private Integer hr;
        private BigDecimal ops;
        @Schema(description = "타석", example = "520")
        private Integer pa;
    }

    @Schema(description = "투수 주요 스탯")
    @Builder
    @Getter
    public static class PitcherStatDto {
        @Schema(description = "보직 (STARTER: 선발, RELIEVER: 불펜)", example = "STARTER")
        private String role;
        private String playerName;
        private BigDecimal era;
        private Integer w;
        private Integer so;
        @Schema(description = "이닝", example = "165.33")
        private BigDecimal ip;
        @Schema(description = "세이브", example = "0")
        private Integer sv;
        @Schema(description = "홀드", example = "0")
        private Integer hld;
    }
}
