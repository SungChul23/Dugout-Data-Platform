package com.dev.dugout.infrastructure.ml.entity;

import jakarta.persistence.*;
import lombok.*;
import com.dev.dugout.domain.player.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_result")
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

    @Column(nullable = false)
    private String targetSeason; // "2026"

    // 핵심 지표 분리 (정렬/조회 최적화)
    @Column(precision = 5, scale = 3)
    private BigDecimal predAvg;      // 예상 타율

    private Integer predHr;      // 예상 홈런

    @Column(precision = 5, scale = 3)
    private BigDecimal predOps;      // 예상 OPS

    // 전후비교
    @Column(precision = 5, scale = 3)
    private BigDecimal avgDiff;

    private Integer hrDiff;

    @Column(precision = 5, scale = 3)
    private BigDecimal opsDiff;

    //투수 지표
    @Column(precision = 5, scale = 3)
    private BigDecimal predEra;

    @Column(precision = 5, scale = 3)
    private BigDecimal predWhip;

    @Column(precision = 5, scale = 3)
    private BigDecimal eraEliteProb;

    @Column(precision = 5, scale = 3)
    private BigDecimal whipEliteProb;


    @Column(columnDefinition = "TEXT")
    private String insightJson;  // Bedrock 상세 분석용 원본 데이터

    private LocalDateTime predictedAt;
}

