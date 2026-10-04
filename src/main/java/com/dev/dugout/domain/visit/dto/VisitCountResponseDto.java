package com.dev.dugout.domain.visit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Schema(description = "오늘 방문자 수 응답 DTO")
@Builder
@Getter
public class VisitCountResponseDto {

    @Schema(description = "기준 날짜 (한국 시간)", example = "2026-10-05")
    private LocalDate date;

    @Schema(description = "해당 날짜의 순 방문자 수 (IP 기준 중복 제거)", example = "128")
    private Long count;
}
