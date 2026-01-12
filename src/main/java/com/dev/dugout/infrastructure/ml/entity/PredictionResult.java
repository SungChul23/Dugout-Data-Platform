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

    private String targetSeason; //2026
    @Column(columnDefinition = "TEXT")
    private String predictionData; // JSON 형식
    private Integer confidence;    // 예측 신뢰도 80%, 85% ..

}
