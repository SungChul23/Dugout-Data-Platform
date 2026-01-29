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
    private String faStatus;

    // 지표
    private BigDecimal statPitching; // 투수 - 구위 점수
    private BigDecimal statStability; // 투수 - 안정성 점수
    private BigDecimal statOffense; // 타자 - 공격 점수
    private BigDecimal statDefense; // 타자 - 수비 점수
    private BigDecimal statContribution; // 공통 - 기여도 점수
}
