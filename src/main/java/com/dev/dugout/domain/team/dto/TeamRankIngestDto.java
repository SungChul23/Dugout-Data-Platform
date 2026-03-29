package com.dev.dugout.domain.team.dto;

import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.Team;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRankIngestDto {

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("games_played")
    private Integer gamesPlayed;

    @JsonProperty("wins")
    private Integer wins;

    @JsonProperty("losses")
    private Integer losses;

    @JsonProperty("draws")
    private Integer draws;

    @JsonProperty("win_rate")
    private BigDecimal winRate;

    @JsonProperty("games_behind")
    private BigDecimal gamesBehind; // 0.5 차이 등을 위해 BigDecimal 사용

    @JsonProperty("last_10_games")
    private String last10Games;

    @JsonProperty("streak")
    private String streak;

    @JsonProperty("home_record")
    private String homeRecord;

    @JsonProperty("away_record")
    private String awayRecord;

    /**
     * DTO를 DailyTeamRanking 엔티티로 변환
     */
    public DailyTeamRanking toEntity(LocalDate baseDate, Team team) {
        return DailyTeamRanking.builder()
                .base_date(baseDate)
                .team(team)
                .rank(this.rank)
                .gamesPlayed(this.gamesPlayed)
                .wins(this.wins)
                .losses(this.losses)
                .draws(this.draws)
                .winRate(this.winRate)
                .gamesBehind(this.gamesBehind)
                .last10Games(this.last10Games)
                .streak(this.streak)
                .homeRecord(this.homeRecord)
                .awayRecord(this.awayRecord)
                .build();
    }

}