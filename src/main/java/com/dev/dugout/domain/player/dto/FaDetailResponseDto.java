package com.dev.dugout.domain.player.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class FaDetailResponseDto {

    //선수 정보
    private Long playerId;
    private String playerName;
    private String subPositionType;
    private Integer age;
    private String playerIntro;
    private String grade;
    private String currentSalary;
    private String aiFeedback;

    // 지표
    private BigDecimal statPitching;
    private BigDecimal statStability;
    private BigDecimal statOffense;
    private BigDecimal statDefense;
    private BigDecimal statContribution;
}
