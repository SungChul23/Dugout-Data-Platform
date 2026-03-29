package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(TeamRankingId.class)
@Table(name = "daily_team_ranking")
public class DailyTeamRanking {


    @Id
    @Column(name = "base_date")
    private LocalDate baseDate; // JSON: base_date

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "team_rank")
    private Integer rank;          // JSON: rank

    private Integer gamesPlayed;   // JSON: games_played
    private Integer wins;
    private Integer losses;
    private Integer draws;

    @Column(precision = 5, scale = 3)
    private BigDecimal winRate;    // JSON: win_rate

    @Column(precision = 4, scale = 1)
    private BigDecimal gamesBehind; // JSON: games_behind (예: 2.5)

    private String last10Games;    // JSON: last_10_games
    private String streak;
    private String homeRecord;     // JSON: home_record
    private String awayRecord;     // JSON: away_record

    private Integer aiPredictedRank; // AWS 예측 데이터 (초기 null)
}
