package com.dev.dugout.domain.player.entity;

import com.dev.dugout.domain.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(DailyPlayerPitcherId.class)
@Table(name = "daily_player_pitcher")
public class DailyPlayerPitcher {

    @Id
    @Column(name = "base_date")
    private LocalDate baseDate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player; // Player 테이블의 kbo_pcode와 조인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private String playerName; // 단순 조회용

    // --- [Basic 1 지표] ---
    @Column(name = "p_era", precision = 6, scale = 2) private BigDecimal era;     // 평균자책점
    @Column(name = "p_g") private Integer g;        // 경기
    @Column(name = "p_w") private Integer w;        // 승
    @Column(name = "p_l") private Integer l;        // 패
    @Column(name = "p_sv") private Integer sv;      // 세이브
    @Column(name = "p_hld") private Integer hld;    // 홀드
    @Column(name = "p_wpct", precision = 5, scale = 3) private BigDecimal wpct;   // 승률
    @Column(name = "p_ip", precision = 5, scale = 2) private BigDecimal ip;       // 이닝 (ex: 5.33)
    @Column(name = "p_h") private Integer h;        // 피안타
    @Column(name = "p_hr") private Integer hr;      // 피홈런
    @Column(name = "p_bb") private Integer bb;      // 볼넷
    @Column(name = "p_hbp") private Integer hbp;    // 사구
    @Column(name = "p_so") private Integer so;      // 탈삼진
    @Column(name = "p_r") private Integer r;        // 실점
    @Column(name = "p_er") private Integer er;      // 자책점
    @Column(name = "p_whip", precision = 5, scale = 2) private BigDecimal whip;   // WHIP

    // --- [Basic 2 지표] ---
    @Column(name = "p_cg") private Integer cg;      // 완투
    @Column(name = "p_sho") private Integer sho;    // 완봉
    @Column(name = "p_qs") private Integer qs;      // 퀄리티스타트
    @Column(name = "p_bsv") private Integer bsv;    // 블론세이브
    @Column(name = "p_tbf") private Integer tbf;    // 타자수
    @Column(name = "p_np") private Integer np;      // 투구수
    @Column(name = "p_avg", precision = 5, scale = 3) private BigDecimal avg;     // 피안타율
    @Column(name = "p_2b") private Integer b2;      // 피2루타
    @Column(name = "p_3b") private Integer b3;      // 피3루타
    @Column(name = "p_sac") private Integer sac;    // 희생번트 허용
    @Column(name = "p_sf") private Integer sf;      // 희생플라이 허용
    @Column(name = "p_ibb") private Integer ibb;    // 고의4구 허용
    @Column(name = "p_wp") private Integer wp;      // 폭투
    @Column(name = "p_bk") private Integer bk;      // 보크

    // --- [Detail 1 지표] ---
    @Column(name = "p_gs") private Integer gs;      // 선발경기
    @Column(name = "p_wgs") private Integer wgs;    // 선발승
    @Column(name = "p_wgr") private Integer wgr;    // 구원승
    @Column(name = "p_gf") private Integer gf;      // 경기종료(투수)
    @Column(name = "p_svo") private Integer svo;    // 세이브기회
    @Column(name = "p_ts") private Integer ts;      // 터프세이브
    @Column(name = "p_gdp") private Integer gdp;    // 병살타 유도
    @Column(name = "p_go") private Integer go;      // 땅볼
    @Column(name = "p_ao") private Integer ao;      // 뜬공
    @Column(name = "p_go_ao", precision = 5, scale = 2) private BigDecimal goAo; // 땅볼/뜬공 비율
}