package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(TeamStatsId.class)
@Table(name = "daily_team_stats")
public class DailyTeamStats {

    @Id
    @Column(name = "base_date")
    private LocalDate baseDate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // --- [타격 1: h1] ---
    @Column(name = "rank_h1") private Integer rankh1;
    @Column(name = "avg_h1", precision = 5, scale = 3) private BigDecimal avgh1;
    @Column(name = "g_h1") private Integer gh1;
    @Column(name = "pa_h1") private Integer pah1;
    @Column(name = "ab_h1") private Integer abh1;
    @Column(name = "r_h1") private Integer rh1;
    @Column(name = "h_h1") private Integer hh1;
    @Column(name = "b2_h1") private Integer b2h1;
    @Column(name = "b3_h1") private Integer b3h1;
    @Column(name = "hr_h1") private Integer hrh1;
    @Column(name = "tb_h1") private Integer tbh1;
    @Column(name = "rbi_h1") private Integer rbih1;
    @Column(name = "sac_h1") private Integer sach1;
    @Column(name = "sf_h1") private Integer sfh1;

    // --- [타격 2: h2] ---
    @Column(name = "rank_h2") private Integer rankh2;
    @Column(name = "avg_h2", precision = 5, scale = 3) private BigDecimal avgh2;
    @Column(name = "bb_h2") private Integer bbh2;
    @Column(name = "ibb_h2") private Integer ibbh2;
    @Column(name = "hbp_h2") private Integer hbph2;
    @Column(name = "so_h2") private Integer soh2;
    @Column(name = "gdp_h2") private Integer gdph2;
    @Column(name = "slg_h2", precision = 5, scale = 3) private BigDecimal slgh2;
    @Column(name = "obp_h2", precision = 5, scale = 3) private BigDecimal obph2;
    @Column(name = "ops_h2", precision = 5, scale = 3) private BigDecimal opsh2;
    @Column(name = "mh_h2") private Integer mhh2;
    @Column(name = "risp_h2", precision = 5, scale = 3) private BigDecimal risph2;
    @Column(name = "ph_ba_h2", precision = 5, scale = 3) private BigDecimal phbah2;

    // --- [투구 1: p1] ---
    @Column(name = "rank_p1") private Integer rankp1;
    @Column(name = "era_p1", precision = 6, scale = 2) private BigDecimal erap1;
    @Column(name = "g_p1") private Integer gp1;
    @Column(name = "w_p1") private Integer wp1;
    @Column(name = "l_p1") private Integer lp1;
    @Column(name = "sv_p1") private Integer svp1;
    @Column(name = "hld_p1") private Integer hldp1;
    @Column(name = "wpct_p1", precision = 5, scale = 3) private BigDecimal wpctp1;
    @Column(name = "ip_p1", precision = 7, scale = 2) private BigDecimal ipp1;
    @Column(name = "h_p1") private Integer hp1;
    @Column(name = "hr_p1") private Integer hrp1;
    @Column(name = "bb_p1") private Integer bbp1;
    @Column(name = "hbp_p1") private Integer hbpp1;
    @Column(name = "so_p1") private Integer sop1;
    @Column(name = "r_p1") private Integer rp1;
    @Column(name = "er_p1") private Integer erp1;
    @Column(name = "whip_p1", precision = 5, scale = 2) private BigDecimal whipp1;

    // --- [투구 2: p2] ---
    @Column(name = "rank_p2") private Integer rankp2;
    @Column(name = "era_p2", precision = 6, scale = 2) private BigDecimal erap2;
    @Column(name = "cg_p2") private Integer cgp2;
    @Column(name = "sho_p2") private Integer shop2;
    @Column(name = "qs_p2") private Integer qsp2;
    @Column(name = "bsv_p2") private Integer bsvp2;
    @Column(name = "tbf_p2") private Integer tbfp2;
    @Column(name = "np_p2") private Integer npp2;
    @Column(name = "avg_p2", precision = 5, scale = 3) private BigDecimal avgp2;
    @Column(name = "b2_p2") private Integer b2p2;
    @Column(name = "b3_p2") private Integer b3p2;
    @Column(name = "sac_p2") private Integer sacp2;
    @Column(name = "sf_p2") private Integer sfp2;
    @Column(name = "wp_p2") private Integer wpp2;
    @Column(name = "bk_p2") private Integer bkp2;

    // --- [수비: d] ---
    @Column(name = "rank_d") private Integer rankd;
    @Column(name = "g_d") private Integer gd;
    @Column(name = "e_d") private Integer ed;
    @Column(name = "pko_d") private Integer pkod;
    @Column(name = "po_d") private Integer pod;
    @Column(name = "a_d") private Integer ad;
    @Column(name = "dp_d") private Integer dpd;
    @Column(name = "fpct_d", precision = 5, scale = 3) private BigDecimal fpctd;
    @Column(name = "pb_d") private Integer pbd;
    @Column(name = "sb_d") private Integer sbd;
    @Column(name = "cs_d") private Integer csd;
    @Column(name = "cs_rate_d", precision = 5, scale = 2) private BigDecimal csrated;

    // --- [주루: r] ---
    @Column(name = "rank_r") private Integer rankr;
    @Column(name = "g_r") private Integer gr;
    @Column(name = "sba_r") private Integer sbar;
    @Column(name = "sb_r") private Integer sbr;
    @Column(name = "cs_r") private Integer csr;
    @Column(name = "sb_rate_r", precision = 5, scale = 2) private BigDecimal sbrater;
    @Column(name = "oob_r") private Integer oobr;
    @Column(name = "pko_r") private Integer pkor;
}