package com.dev.dugout.domain.player.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaListResponseDto {

    private Long id; // 선수 아이디
    private String playerName; // 선수명
    private String subPositionType; // 주포지션 ex ss, 1B
    private Integer age; // 선수 나이
    private String grade; // 선수 등급
    private String currentSalary; //계약
    private String playerIntro; // 선수 한 줄 소개
    private String faStatus; // fa 상태 (잔류,미정,영입,예정)
}
