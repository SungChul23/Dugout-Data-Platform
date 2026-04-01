package com.dev.dugout.domain.team.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameResponseDto {
    private Long id;
    private String date; // 프론트 요구사항: YYYY-MM-DD
    private String time; // HH:mm
    private String home; // 팀 한글 풀네임
    private String away; // 팀 한글 풀네임
    //  점수 필드 추가
    private Integer homeScore;
    private Integer awayScore;

    private String stadium;
    private String status; // SCHEDULED, LIVE, FINISHED, CANCELED
}
