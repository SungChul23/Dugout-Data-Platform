package com.dev.dugout.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import com.dev.dugout.domain.player.entity.Player;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_dashboard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//사용자 맞춤형 대시보드
//회원가입 시에는 비어있다가, 유저가 선수를 추가하면 레코드가 생성
public class UserDashboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dashboardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // 대시보드 상의 위치 (1, 2, 3번 슬롯 구분용)
    @Column(nullable = false)
    private Integer slotNumber;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}