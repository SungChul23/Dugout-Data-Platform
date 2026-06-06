package com.dev.dugout.domain.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "FA 선수 목록 응답 DTO")
@Getter
@Builder
public class FaListResponseDto {

    @Schema(description = "선수 고유 ID (KBO 선수 코드)", example = "79047")
    private Long id;

    @Schema(description = "선수명", example = "김도영")
    private String playerName;

    @Schema(description = "주 포지션 코드", example = "SS")
    private String subPositionType;

    @Schema(description = "선수 나이", example = "24")
    private Integer age;

    @Schema(description = "ML 모델 기반 FA 등급 (A: 최상위 / B: 중상위 / C: 중위)", example = "A")
    private String grade;

    @Schema(description = "현재 연봉 또는 계약 정보", example = "5억원")
    private String currentSalary;

    @Schema(description = "선수 한 줄 소개", example = "KIA의 핵심 유격수. 공격과 수비를 겸비한 차세대 에이스.")
    private String playerIntro;

    @Schema(description = "FA 상태 (미정 / 영입 / 예정)", example = "미정")
    private String faStatus;
}
