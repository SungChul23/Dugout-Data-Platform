package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "경기 일정 및 결과 응답 DTO")
@Getter
@Builder
public class GameResponseDto {

    @Schema(description = "경기 고유 ID", example = "20260606001")
    private Long id;

    @Schema(description = "경기 날짜 (YYYY-MM-DD)", example = "2026-06-06")
    private String date;

    @Schema(description = "경기 시작 시간 (HH:mm)", example = "18:30")
    private String time;

    @Schema(description = "홈 팀명 (한글 풀네임)", example = "KIA 타이거즈")
    private String home;

    @Schema(description = "원정 팀명 (한글 풀네임)", example = "LG 트윈스")
    private String away;

    @Schema(description = "홈 팀 득점 (경기 종료 후 제공)", example = "5")
    private Integer homeScore;

    @Schema(description = "원정 팀 득점 (경기 종료 후 제공)", example = "3")
    private Integer awayScore;

    @Schema(description = "경기 구장명", example = "광주-기아 챔피언스 필드")
    private String stadium;

    @Schema(description = "경기 상태 (SCHEDULED: 예정 / LIVE: 진행 중 / FINISHED: 종료 / CANCELED: 취소)", example = "FINISHED")
    private String status;
}
