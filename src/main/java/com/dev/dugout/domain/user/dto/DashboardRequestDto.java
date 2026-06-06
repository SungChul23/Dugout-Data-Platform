package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "대시보드 선수 추가 요청 DTO")
@Data
public class DashboardRequestDto {

    @Schema(description = "추가할 선수 고유 ID (KBO 선수 코드)", example = "79047")
    private Long playerId;

    @Schema(description = "슬롯 번호 (1~3)", example = "1")
    private int slotNumber;
}
