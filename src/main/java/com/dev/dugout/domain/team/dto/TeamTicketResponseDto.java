package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Schema(description = "구단 티켓 예매 정보 응답 DTO")
@Data
@AllArgsConstructor
@Builder
public class TeamTicketResponseDto {

    @Schema(description = "팀 고유 ID", example = "5")
    private Long id;

    @Schema(description = "팀명", example = "KIA 타이거즈")
    private String name;

    @Schema(description = "연고지", example = "광주")
    private String city;

    @Schema(description = "홈구장 이름", example = "광주-기아 챔피언스 필드")
    private String stadiumName;

    @Schema(description = "공식 티켓 예매 URL")
    private String bookingUrl;
}
