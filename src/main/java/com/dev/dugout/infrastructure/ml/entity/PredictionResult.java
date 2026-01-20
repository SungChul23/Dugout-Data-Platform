package com.dev.dugout.infrastructure.ml.entity;

import jakarta.persistence.*;
import lombok.*;
import com.dev.dugout.domain.player.entity.Player;

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
    private Double predAvg;      // 예상 타율
    private Integer predHr;      // 예상 홈런
    private Double predOps;      // 예상 OPS

    // 변화폭 저장 (UI에서 화살표 및 수치 표시용)
    private Double avgDiff;
    private Integer hrDiff;
    private Double opsDiff;

    @Column(columnDefinition = "TEXT")
    private String insightJson;  // Bedrock 상세 분석용 원본 데이터

    private LocalDateTime predictedAt;
}

