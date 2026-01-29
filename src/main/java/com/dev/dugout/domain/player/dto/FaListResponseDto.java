package com.dev.dugout.domain.player.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaListResponseDto {

    private Long id;              // dto.playerId || dto.id 대응
    private String playerName;    // dto.playerName 대응
    private String subPositionType; // dto.subPositionType 대응
    private Integer age;
    private String grade;
    private String currentSalary;
    private String playerIntro;    // dto.playerIntro 대응
}
