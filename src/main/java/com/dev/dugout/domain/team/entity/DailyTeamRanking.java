package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(TeamRankingId.class) //복합키 생성

//추후 월요일마다 각 팀의 상성 분석 후 최신화된 팀 예측 인사이트 제공 + 팀 순위 인사이트
//특정 시점의 상태를 저장하는 스냅샷(Snapshot) 데이터
public class DailyTeamRanking {
    @Id
    private LocalDate rankingDate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // --- 팀 순위 컬럼들을 반영한 필드 ---
    @Column(name = "team_rank")
    private Integer rank;          // 순위

    private Integer totalGames;    // 경기수
    private Integer wins;          // 승
    private Integer losses;        // 패
    private Integer draws;         // 무

    @Column(precision = 5, scale = 3)
    private BigDecimal winRate;        // 승률

    private Double gamesBehind;    // 게임차
    private String recent10Games;  // 최근 10경기 (예: 5승1무4패)
    private String streak;         // 연속 (예: 3패, 1승)
    private String homeRecord;     // 홈 기록 (예: 41-1-29)
    private String awayRecord;     // 방문 기록 (예: 44-2-27)

    // --- 프로젝트 고도화 필드 ---
    private Integer aiPredictedRank; // AWS DEA 활용 예측 데이터

}
