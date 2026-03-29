package com.dev.dugout.domain.player.dto;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.team.entity.Team;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitcherIngestDto {

    @JsonProperty("p_pcode")
    private String pcode; // String 처리 ✅

    @JsonProperty("선수명")
    private String playerName;

    @JsonProperty("team_id")
    private Long teamId;

    // --- [Basic 1 지표] ---
    @JsonProperty("p_era") private BigDecimal era;
    @JsonProperty("p_g") private Integer g;
    @JsonProperty("p_w") private Integer w;
    @JsonProperty("p_l") private Integer l;
    @JsonProperty("p_sv") private Integer sv;
    @JsonProperty("p_hld") private Integer hld;
    @JsonProperty("p_wpct") private BigDecimal wpct;
    @JsonProperty("p_ip") private BigDecimal ip;
    @JsonProperty("p_h") private Integer h;
    @JsonProperty("p_hr") private Integer hr;
    @JsonProperty("p_bb") private Integer bb;
    @JsonProperty("p_hbp") private Integer hbp;
    @JsonProperty("p_so") private Integer so;
    @JsonProperty("p_r") private Integer r;
    @JsonProperty("p_er") private Integer er;
    @JsonProperty("p_whip") private BigDecimal whip;

    // --- [Basic 2 지표] ---
    @JsonProperty("p_cg") private Integer cg;
    @JsonProperty("p_sho") private Integer sho;
    @JsonProperty("p_qs") private Integer qs;
    @JsonProperty("p_bsv") private Integer bsv;
    @JsonProperty("p_tbf") private Integer tbf;
    @JsonProperty("p_np") private Integer np;
    @JsonProperty("p_avg") private BigDecimal avg;
    @JsonProperty("p_2b") private Integer b2;
    @JsonProperty("p_3b") private Integer b3;
    @JsonProperty("p_sac") private Integer sac;
    @JsonProperty("p_sf") private Integer sf;
    @JsonProperty("p_ibb") private Integer ibb;
    @JsonProperty("p_wp") private Integer wp;
    @JsonProperty("p_bk") private Integer bk;

    // --- [Detail 1 지표] ---
    @JsonProperty("p_gs") private Integer gs;
    @JsonProperty("p_wgs") private Integer wgs;
    @JsonProperty("p_wgr") private Integer wgr;
    @JsonProperty("p_gf") private Integer gf;
    @JsonProperty("p_svo") private Integer svo;
    @JsonProperty("p_ts") private Integer ts;
    @JsonProperty("p_gdp") private Integer gdp;
    @JsonProperty("p_go") private Integer go;
    @JsonProperty("p_ao") private Integer ao;
    @JsonProperty("p_go_ao") private BigDecimal goAo;

    /**
     * DTO를 투수 성적 엔티티로 변환
     */
    public DailyPlayerPitcher toEntity(LocalDate baseDate, Player player, Team team) {
        return DailyPlayerPitcher.builder()
                .baseDate(baseDate)
                .player(player)
                .team(team)
                .playerName(this.playerName)
                .era(this.era)
                .g(this.g)
                .w(this.w)
                .l(this.l)
                .sv(this.sv)
                .hld(this.hld)
                .wpct(this.wpct)
                .ip(this.ip)
                .h(this.h)
                .hr(this.hr)
                .bb(this.bb)
                .hbp(this.hbp)
                .so(this.so)
                .r(this.r)
                .er(this.er)
                .whip(this.whip)
                .cg(this.cg)
                .sho(this.sho)
                .qs(this.qs)
                .bsv(this.bsv)
                .tbf(this.tbf)
                .np(this.np)
                .avg(this.avg)
                .b2(this.b2)
                .b3(this.b3)
                .sac(this.sac)
                .sf(this.sf)
                .ibb(this.ibb)
                .wp(this.wp)
                .bk(this.bk)
                .gs(this.gs)
                .wgs(this.wgs)
                .wgr(this.wgr)
                .gf(this.gf)
                .svo(this.svo)
                .ts(this.ts)
                .gdp(this.gdp)
                .go(this.go)
                .ao(this.ao)
                .goAo(this.goAo)
                .build();
    }
}