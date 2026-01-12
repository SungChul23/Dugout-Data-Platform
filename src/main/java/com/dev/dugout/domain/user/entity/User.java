package com.dev.dugout.domain.user.entity;


import com.dev.dugout.domain.team.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId; // 로그인 아이디

    @Column(nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(nullable = false, length = 50)
    private String nickname; // 서비스 닉네임

    // User.java 내부
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // DB에는 숫자(ID)로 저장되지만 객체로 다룹니다.
    private Team favoriteTeam;

    private LocalDateTime createdAt;
    @PrePersist // 데이터가 저장되기 직전에 가입 시간 자동 생성
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
