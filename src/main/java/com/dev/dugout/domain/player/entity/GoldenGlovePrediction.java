package com.dev.dugout.domain.player.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "gg_leaderboard")
@Getter
@NoArgsConstructor
public class GoldenGlovePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "player_code", length = 10, nullable = false)
    private String playerCode;

    @Column(name = "player_name", length = 50)
    private String playerName;

    @Column(name = "team_name", length = 50)
    private String teamName;

    @Column(name = "position", length = 10)
    private String position;

    @Column(name = "win_prob")
    private Double winProb;

    @Column(name = "win_prob_str", length = 10)
    private String winProbStr;

    @Column(name = "rank_in_pos")
    private Integer rank;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top3_positive", columnDefinition = "json")
    private List<Map<String, Object>> top3Positive;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top1_negative", columnDefinition = "json")
    private List<Map<String, Object>> top1Negative;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Builder
    public GoldenGlovePrediction(LocalDate baseDate, String playerCode, String playerName, String teamName, String position, Double winProb, String winProbStr, Integer rank, List<Map<String, Object>> top3Positive, List<Map<String, Object>> top1Negative, String aiExplanation) {
        this.baseDate = baseDate;
        this.playerCode = playerCode;
        this.playerName = playerName;
        this.teamName = teamName;
        this.position = position;
        this.winProb = winProb;
        this.winProbStr = winProbStr;
        this.rank = rank;
        this.top3Positive = top3Positive;
        this.top1Negative = top1Negative;
        this.aiExplanation = aiExplanation;
    }
}