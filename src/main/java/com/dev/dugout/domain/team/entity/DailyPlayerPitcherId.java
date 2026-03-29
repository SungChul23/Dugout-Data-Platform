package com.dev.dugout.domain.team.entity;

import java.io.Serializable;
import lombok.*;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
//DailyPlayerPitcher 복합키 생성
public class DailyPlayerPitcherId implements Serializable {
    private LocalDate baseDate;
    private Integer player;
}