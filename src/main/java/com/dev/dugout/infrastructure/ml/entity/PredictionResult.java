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
    private Player player; // player 테이블에서의 선수 코드를 참조

    private String targetSeason; // 2026

    @Column(columnDefinition = "TEXT") // JSON 형태로 저장
    private String predictionData;

    private LocalDateTime predictedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isBatch = false; // 고정 선수 예측(ture) 및 사용자 선정 선수 예측(false)
}
