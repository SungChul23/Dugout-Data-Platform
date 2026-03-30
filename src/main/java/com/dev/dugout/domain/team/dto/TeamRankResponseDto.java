package com.dev.dugout.domain.team.dto;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
public class TeamRankResponseDto {

    private LocalDate rankingDate;    // date
    private Long teamId;             // bigint
    private String teamName;         // 팀 식별을 위한 이름 추가 (선택사항)
    private String awayRecord;       // varchar(255)
    private Integer draws;           // int
    private BigDecimal gamesBehind;  // decimal(4,1)
    private String homeRecord;       // varchar(255)
    private Integer losses;          // int
    private Integer teamRank;        // int
    private String recent10games;    // varchar(255)
    private String streak;           // varchar(255)
    private Integer totalGames;      // int
    private BigDecimal winRate;      // decimal(5,3)
    private Integer wins;            // int
    private Integer gamesPlayed;     // int
    private String last10games;      // varchar(255)
}
