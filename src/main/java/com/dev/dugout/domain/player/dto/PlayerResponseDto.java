package com.dev.dugout.domain.player.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "구단별 선수 명단 응답 DTO")
@Getter
@Builder
public class PlayerResponseDto {

    @Schema(description = "선수 고유 ID (KBO 선수 코드)", example = "79047")
    private Long playerId;

    @Schema(description = "선수명", example = "김도영")
    private String name;

    @Schema(description = "등번호", example = "5")
    @JsonProperty("back_number")
    private Integer backNumber;

    @Schema(description = "포지션 타입 (hitter: 타자 / pitcher: 투수)", example = "hitter")
    @JsonProperty("position_type")
    private String positionType;
}
