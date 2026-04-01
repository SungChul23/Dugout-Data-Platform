package com.dev.dugout.infrastructure.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResultIngestDto {

    @JsonProperty("game_date")
    private String gameDate;

    @JsonProperty("game_time")
    private String gameTime;

    @JsonProperty("home_score")
    private Integer homeScore;

    @JsonProperty("away_score")
    private Integer awayScore;

    @JsonProperty("status")
    private String status; // SCHEDULED, LIVE, FINISHED, CANCELED

    @JsonProperty("home_team_id")
    private Long homeTeamId;

    @JsonProperty("away_team_id")
    private Long awayTeamId;
}