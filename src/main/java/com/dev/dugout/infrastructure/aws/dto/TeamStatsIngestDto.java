package com.dev.dugout.infrastructure.aws.dto;

import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.entity.Team;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamStatsIngestDto {

    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("팀명")
    private String teamName;

    @JsonProperty("base_date")
    private String baseDate;

    // --- [타격 1: h1] ---
    @JsonProperty("순위_h1") private Integer rankh1;
    @JsonProperty("avg_h1") private BigDecimal avgh1;
    @JsonProperty("g_h1") private Integer gh1;
    @JsonProperty("pa_h1") private Integer pah1;
    @JsonProperty("ab_h1") private Integer abh1;
    @JsonProperty("r_h1") private Integer rh1;
    @JsonProperty("h_h1") private Integer hh1;
    @JsonProperty("2b_h1") private Integer b2h1;
    @JsonProperty("3b_h1") private Integer b3h1;
    @JsonProperty("hr_h1") private Integer hrh1;
    @JsonProperty("tb_h1") private Integer tbh1;
    @JsonProperty("rbi_h1") private Integer rbih1;
    @JsonProperty("sac_h1") private Integer sach1;
    @JsonProperty("sf_h1") private Integer sfh1;

    // --- [타격 2: h2] ---
    @JsonProperty("순위_h2") private Integer rankh2;
    @JsonProperty("avg_h2") private BigDecimal avgh2; // 추가 ✅
    @JsonProperty("bb_h2") private Integer bbh2;
    @JsonProperty("ibb_h2") private Integer ibbh2;
    @JsonProperty("hbp_h2") private Integer hbph2;
    @JsonProperty("so_h2") private Integer soh2;
    @JsonProperty("gdp_h2") private Integer gdph2;
    @JsonProperty("slg_h2") private BigDecimal slgh2;
    @JsonProperty("obp_h2") private BigDecimal obph2;
    @JsonProperty("ops_h2") private BigDecimal opsh2;
    @JsonProperty("mh_h2") private Integer mhh2;
    @JsonProperty("risp_h2") private BigDecimal risph2;
    @JsonProperty("ph_ba_h2") private BigDecimal phbah2;

    // --- [투구 1: p1] ---
    @JsonProperty("순위_p1") private Integer rankp1;
    @JsonProperty("era_p1") private BigDecimal erap1;
    @JsonProperty("g_p1") private Integer gp1; // 교정 완료 ✅
    @JsonProperty("w_p1") private Integer wp1;
    @JsonProperty("l_p1") private Integer lp1;
    @JsonProperty("sv_p1") private Integer svp1;
    @JsonProperty("hld_p1") private Integer hldp1;
    @JsonProperty("wpct_p1") private BigDecimal wpctp1;
    @JsonProperty("ip_p1") private BigDecimal ipp1;
    @JsonProperty("h_p1") private Integer hp1;
    @JsonProperty("hr_p1") private Integer hrp1;
    @JsonProperty("bb_p1") private Integer bbp1;
    @JsonProperty("hbp_p1") private Integer hbpp1;
    @JsonProperty("so_p1") private Integer sop1;
    @JsonProperty("r_p1") private Integer rp1;
    @JsonProperty("er_p1") private Integer erp1;
    @JsonProperty("whip_p1") private BigDecimal whipp1;

    // --- [투구 2: p2] ---
    @JsonProperty("순위_p2") private Integer rankp2;
    @JsonProperty("era_p2") private BigDecimal erap2; // 추가 ✅
    @JsonProperty("cg_p2") private Integer cgp2;
    @JsonProperty("sho_p2") private Integer shop2;
    @JsonProperty("qs_p2") private Integer qsp2;
    @JsonProperty("bsv_p2") private Integer bsvp2;
    @JsonProperty("tbf_p2") private Integer tbfp2;
    @JsonProperty("np_p2") private Integer npp2;
    @JsonProperty("avg_p2") private BigDecimal avgp2;
    @JsonProperty("2b_p2") private Integer b2p2;
    @JsonProperty("3b_p2") private Integer b3p2;
    @JsonProperty("sac_p2") private Integer sacp2;
    @JsonProperty("sf_p2") private Integer sfp2; // 추가 ✅
    @JsonProperty("wp_p2") private Integer wpp2;
    @JsonProperty("bk_p2") private Integer bkp2;

    // --- [수비: d] ---
    @JsonProperty("순위_d") private Integer rankd;
    @JsonProperty("g_d") private Integer gd; // 추가 ✅
    @JsonProperty("e_d") private Integer ed;
    @JsonProperty("pko_d") private Integer pkod;
    @JsonProperty("po_d") private Integer pod;
    @JsonProperty("a_d") private Integer ad;
    @JsonProperty("dp_d") private Integer dpd;
    @JsonProperty("fpct_d") private BigDecimal fpctd;
    @JsonProperty("pb_d") private Integer pbd;
    @JsonProperty("sb_d") private Integer sbd; // 추가 ✅
    @JsonProperty("cs_d") private Integer csd; // 추가 ✅
    @JsonProperty("cs_rate_d") private BigDecimal csrated;

    // --- [주루: r] ---
    @JsonProperty("순위_r") private Integer rankr;
    @JsonProperty("g_r") private Integer gr; // 추가 ✅
    @JsonProperty("sba_r") private Integer sbar;
    @JsonProperty("sb_r") private Integer sbr;
    @JsonProperty("cs_r") private Integer csr;
    @JsonProperty("sb_rate_r") private BigDecimal sbrater;
    @JsonProperty("oob_r") private Integer oobr;
    @JsonProperty("pko_r") private Integer pkor;

    public DailyTeamStats toEntity(LocalDate baseDate, Team team) {
        return DailyTeamStats.builder()
                .baseDate(baseDate)
                .team(team)
                .rankh1(this.rankh1).avgh1(this.avgh1).gh1(this.gh1).pah1(this.pah1).abh1(this.abh1).rh1(this.rh1).hh1(this.hh1).b2h1(this.b2h1).b3h1(this.b3h1).hrh1(this.hrh1).tbh1(this.tbh1).rbih1(this.rbih1).sach1(this.sach1).sfh1(this.sfh1)
                .rankh2(this.rankh2).avgh2(this.avgh2).bbh2(this.bbh2).ibbh2(this.ibbh2).hbph2(this.hbph2).soh2(this.soh2).gdph2(this.gdph2).slgh2(this.slgh2).obph2(this.obph2).opsh2(this.opsh2).mhh2(this.mhh2).risph2(this.risph2).phbah2(this.phbah2)
                .rankp1(this.rankp1).erap1(this.erap1).gp1(this.gp1).wp1(this.wp1).lp1(this.lp1).svp1(this.svp1).hldp1(this.hldp1).wpctp1(this.wpctp1).ipp1(this.ipp1).hp1(this.hp1).hrp1(this.hrp1).bbp1(this.bbp1).hbpp1(this.hbpp1).sop1(this.sop1).rp1(this.rp1).erp1(this.erp1).whipp1(this.whipp1)
                .rankp2(this.rankp2).erap2(this.erap2).cgp2(this.cgp2).shop2(this.shop2).qsp2(this.qsp2).bsvp2(this.bsvp2).tbfp2(this.tbfp2).npp2(this.npp2).avgp2(this.avgp2).b2p2(this.b2p2).b3p2(this.b3p2).sacp2(this.sacp2).sfp2(this.sfp2).wpp2(this.wpp2).bkp2(this.bkp2)
                .rankd(this.rankd).gd(this.gd).ed(this.ed).pkod(this.pkod).pod(this.pod).ad(this.ad).dpd(this.dpd).fpctd(this.fpctd).pbd(this.pbd).sbd(this.sbd).csd(this.csd).csrated(this.csrated)
                .rankr(this.rankr).gr(this.gr).sbar(this.sbar).sbr(this.sbr).csr(this.csr).sbrater(this.sbrater).oobr(this.oobr).pkor(this.pkor)
                .build();
    }
}