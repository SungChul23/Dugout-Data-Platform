package com.dev.dugout.infrastructure.ml.entity;

import jakarta.persistence.*;
import lombok.*;
import com.dev.dugout.domain.player.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_result", indexes = {
        @Index(name = "idx_player_season", columnList = "player_id, target_season")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long predictId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "target_season", nullable = false, length = 10)
    private String targetSeason; // "2026"

    // --- [타자 데이터 (Batting Metrics)] ---

    // 타율 (AVG)
    @Column(precision = 5, scale = 3)
    private BigDecimal currAvg;
    @Column(precision = 5, scale = 3)
    private BigDecimal predAvg;
    @Column(precision = 5, scale = 3)
    private BigDecimal avgDiff;
    @Column(precision = 5, scale = 3)
    private BigDecimal avgMin;
    @Column(precision = 5, scale = 3)
    private BigDecimal avgMax;

    // 출루율 (OBP)
    @Column(precision = 5, scale = 3)
    private BigDecimal currObp;
    @Column(precision = 5, scale = 3)
    private BigDecimal predObp;
    @Column(precision = 5, scale = 3)
    private BigDecimal diffObp;
    @Column(precision = 5, scale = 3)
    private BigDecimal obpMin;
    @Column(precision = 5, scale = 3)
    private BigDecimal obpMax;

    // 장타율 (SLG)
    @Column(precision = 5, scale = 3)
    private BigDecimal currSlg;
    @Column(precision = 5, scale = 3)
    private BigDecimal predSlg;
    @Column(precision = 5, scale = 3)
    private BigDecimal diffSlg;
    @Column(precision = 5, scale = 3)
    private BigDecimal slgMin;
    @Column(precision = 5, scale = 3)
    private BigDecimal slgMax;

    // OPS
    @Column(precision = 5, scale = 3)
    private BigDecimal currOps;
    @Column(precision = 5, scale = 3)
    private BigDecimal predOps;
    @Column(precision = 5, scale = 3)
    private BigDecimal opsDiff;
    @Column(precision = 5, scale = 3)
    private BigDecimal opsMin;
    @Column(precision = 5, scale = 3)
    private BigDecimal opsMax;

    // 홈런 (HR)
    private Integer currHr;
    private Integer predHr;
    private Integer hrDiff;
    private Integer hrMin;
    private Integer hrMax;

    // --- [투수 데이터 (Pitching Metrics)] ---

    @Column(precision = 7, scale = 6)
    private BigDecimal probElite;         // 엘리트 등극 확률

    @Column(precision = 5, scale = 2)
    private BigDecimal rolePercentileTop; // 해당 보직 내 상위 %

    private Integer roleRank;             // 해당 보직 내 순위
    private Integer roleTotal;            // 해당 보직 전체 인원수

    // --- [투수 2025 성적 (Pitching 2025 Performance)] ---
    @Column(name = "era_2025", precision = 4, scale = 2)
    private BigDecimal era2025;

    @Column(name = "fip_2025", precision = 4, scale = 2)
    private BigDecimal fip2025;

    @Column(name = "ip_2025", precision = 6, scale = 3)
    private BigDecimal ip2025;

    @Column(name = "whip_2025", precision = 4, scale = 2)
    private BigDecimal whip2025;

    @Column(name = "role", length = 10)
    private String role;

    // --- [공통 및 분석용 데이터] ---

    @Column(columnDefinition = "TEXT")
    private String insightJson;           // AI 상세 분석 리포트

    @Builder.Default
    private LocalDateTime predictedAt = LocalDateTime.now();
}