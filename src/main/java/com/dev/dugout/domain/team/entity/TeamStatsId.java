package com.dev.dugout.domain.team.entity;

import java.io.Serializable;
import lombok.*;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TeamStatsId implements Serializable {
    private LocalDate baseDate;
    private Long team; // DailyTeamStats의 team 필드 타입과 일치
}