package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
//경기 일정
//실제 야구 경기라는 이벤트(Event)를 기록하는 데이터
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate gameDate; //경기 날짜

    @Column(columnDefinition = "TIME(0)") // 소수점 정밀도를 0으로 설정
    private LocalTime gameTime; //경기 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam; //홈팀 팀 id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam; //원정 팀 id
    private String stadiumName; //실제로 열린 장소

    //추후 값 업데이터
    private Integer homeScore;
    private Integer awayScore;
    private String status;

    // Setter 대신 의미 있는 메서드 사용 (더티 체킹 발생)
    public void updateResult(Integer homeScore, Integer awayScore, String status) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = status;
    }
}