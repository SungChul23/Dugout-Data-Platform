package com.dev.dugout.domain.team.entity;


import com.dev.dugout.domain.player.entity.Player;
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
@IdClass(DailyPlayerHitterId.class)
@Table(name = "daily_player_hitter")

// [타자] 성적 지표 매일 크롤링 (월요일 제외)
public class DailyPlayerHitter {

    @Id
    @Column(name = "base_date")
    private LocalDate baseDate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kbo_pcode", referencedColumnName = "kbo_pcode")
    private Player player; // Player 테이블의 kbo_pcode와 조인 ✅

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private String playerName; // 단순 조회/디버깅용

    // --- [Count 지표: Integer] ---
    @Column(name = "h_g") private Integer g;
    @Column(name = "h_pa") private Integer pa;
    @Column(name = "h_ab") private Integer ab;
    @Column(name = "h_r") private Integer r;
    @Column(name = "h_h") private Integer h;
    @Column(name = "h_2b") private Integer b2;
    @Column(name = "h_3b") private Integer b3;
    @Column(name = "h_hr") private Integer hr;
    @Column(name = "h_tb") private Integer tb;
    @Column(name = "h_rbi") private Integer rbi;
    @Column(name = "h_sac") private Integer sac;
    @Column(name = "h_sf") private Integer sf;
    @Column(name = "h_mh") private Integer mh;      // 멀티히트
    @Column(name = "h_xbh") private Integer xbh;    // 장타
    @Column(name = "h_go") private Integer go;      // 땅볼
    @Column(name = "h_ao") private Integer ao;      // 뜬공
    @Column(name = "h_gw_rbi") private Integer gwRbi; // 결승타

    // --- [Rate/Ratio 지표: BigDecimal] ---
    // 정밀한 계산을 위해 소수점 3자리 이상 보장
    @Column(name = "h_avg", precision = 5, scale = 3) private BigDecimal avg;
    @Column(name = "h_slg", precision = 5, scale = 3) private BigDecimal slg;
    @Column(name = "h_obp", precision = 5, scale = 3) private BigDecimal obp;
    @Column(name = "h_ops", precision = 5, scale = 3) private BigDecimal ops;
    @Column(name = "h_risp", precision = 5, scale = 3) private BigDecimal risp;  // 득점권타율
    @Column(name = "h_ph_ba", precision = 5, scale = 3) private BigDecimal phBa; // 대타타율
    @Column(name = "h_bb_k", precision = 5, scale = 2) private BigDecimal bbK;   // 볼삼비
    @Column(name = "h_isop", precision = 5, scale = 3) private BigDecimal isop;  // 순수장타율
    @Column(name = "h_xr", precision = 7, scale = 2) private BigDecimal xr;      // 득점생산력
    @Column(name = "h_gpa", precision = 5, scale = 3) private BigDecimal gpa;    // GPA
}