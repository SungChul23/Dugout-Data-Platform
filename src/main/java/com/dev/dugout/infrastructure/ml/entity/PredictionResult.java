package com.dev.dugout.infrastructure.ml.entity;

import jakarta.persistence.*;
import lombok.*;
import com.dev.dugout.domain.player.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_result", indexes = {
        @Index(name = "idx_player_season", columnList = "player_id, targetSeason")
        //선수 아이디 + 타켓시즌 인덱싱
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PredictionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long predictId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String targetSeason; // "2026"

    // --- [공통 데이터] ---
    @Column(columnDefinition = "TEXT")
    private String insightJson;

    private LocalDateTime predictedAt;


    // 타율 (AVG) 관련
    @Column(precision = 5, scale = 3)
    private BigDecimal currAvg;      // curr_avg
    @Column(precision = 5, scale = 3)
    private BigDecimal predAvg;      // pred_avg
    @Column(precision = 5, scale = 3)
    private BigDecimal avgDiff;      // avg_diff
    @Column(precision = 5, scale = 3)
    private BigDecimal avgMin;       // avg_min
    @Column(precision = 5, scale = 3)
    private BigDecimal avgMax;       // avg_max

    // 출루율 (OBP) 관련
    @Column(precision = 5, scale = 3)
    private BigDecimal currObp;      // curr_obp_2025
    @Column(precision = 5, scale = 3)
    private BigDecimal predObp;      // pred_obp_2026
    @Column(precision = 5, scale = 3)
    private BigDecimal diffObp;      // diff_obp
    @Column(precision = 5, scale = 3)
    private BigDecimal obpMin;       // obp_min
    @Column(precision = 5, scale = 3)
    private BigDecimal obpMax;       // obp_max

    // 장타율 (SLG) 관련
    @Column(precision = 5, scale = 3)
    private BigDecimal currSlg;      // curr_slg_2025
    @Column(precision = 5, scale = 3)
    private BigDecimal predSlg;      // pred_slg_2026
    @Column(precision = 5, scale = 3)
    private BigDecimal diffSlg;      // diff_slg
    @Column(precision = 5, scale = 3)
    private BigDecimal slgMin;       // slg_min
    @Column(precision = 5, scale = 3)
    private BigDecimal slgMax;       // slg_max

    // OPS 관련
    @Column(precision = 5, scale = 3)
    private BigDecimal currOps;      // curr_ops_2025
    @Column(precision = 5, scale = 3)
    private BigDecimal predOps;      // pred_ops_2026
    @Column(precision = 5, scale = 3)
    private BigDecimal opsDiff;      // diff_ops
    @Column(precision = 5, scale = 3)
    private BigDecimal opsMin;       // ops_min
    @Column(precision = 5, scale = 3)
    private BigDecimal opsMax;       // ops_max

    // 홈런 (HR) 관련
    private Integer currHr;          // curr_hr
    private Integer predHr;          // pred_hr
    private Integer hrDiff;          // hr_diff
    private Integer hrMin;           // hr_min
    private Integer hrMax;           // hr_max


    @Column(precision = 7, scale = 6)
    private BigDecimal probElite;         // prob_elite

    @Column(precision = 5, scale = 2)
    private BigDecimal rolePercentileTop; // role_percentile_top

    private Integer roleRank;             // role_rank
    private Integer roleTotal;            // role_total

}