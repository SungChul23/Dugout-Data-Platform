package com.dev.dugout.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import com.dev.dugout.domain.user.entity.User;

@Entity
@Table(name = "user_prediction_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//계정당 하루 3회 등 제한을 관리하여 SageMaker 호출 비용 방어
public class UserPredictionLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long limitId;

    // 실제 User 엔티티와 다대일(N:1) 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate requestDate; // 예측을 요청한 날짜 (YYYY-MM-DD)

    @Column(nullable = false)
    @Builder.Default
    private Integer requestCount = 0; // 해당 날짜의 요청 횟수

    //요청 횟수에 따라 1증가 (비즈니스 로직에서 사용 될 예정)
    public void incrementCount() {
        this.requestCount++;
    }
}