package com.dev.dugout.domain.player.entity;

import java.io.Serializable;
import lombok.*;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

//DailyPlayerHiiter 복합키 생성
public class DailyPlayerHitterId implements Serializable {
    private LocalDate baseDate;
    private Long player;
}