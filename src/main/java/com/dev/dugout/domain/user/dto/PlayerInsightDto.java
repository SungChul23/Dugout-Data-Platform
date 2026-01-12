package com.dev.dugout.domain.user.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerInsightDto {

    private Integer slotNumber;
    private Long playerId;
    private String name;
    private String position;
    private String predictedStat; // 예: 선수 성적
    private Integer confidence;   // 신뢰도 바
    private boolean isEmpty;      // 비어있는 슬롯인지 여부
}
