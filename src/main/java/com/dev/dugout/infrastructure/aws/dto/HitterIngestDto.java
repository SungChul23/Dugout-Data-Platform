package com.dev.dugout.infrastructure.aws.dto;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
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
public class HitterIngestDto {

    @JsonProperty("h_pcode")
    private String pcode;

    @JsonProperty("선수명")
    private String playerName;

    @JsonProperty("team_id")
    private Long teamId;

    // --- [Count 지표] ---
    @JsonProperty("h_g") private Integer g;
    @JsonProperty("h_pa") private Integer pa;
    @JsonProperty("h_ab") private Integer ab;
    @JsonProperty("h_r") private Integer r;
    @JsonProperty("h_h") private Integer h;
    @JsonProperty("h_2b") private Integer b2;
    @JsonProperty("h_3b") private Integer b3;
    @JsonProperty("h_hr") private Integer hr;
    @JsonProperty("h_tb") private Integer tb;
    @JsonProperty("h_rbi") private Integer rbi;
    @JsonProperty("h_sac") private Integer sac;
    @JsonProperty("h_sf") private Integer sf;
    @JsonProperty("h_mh") private Integer mh;
    @JsonProperty("h_xbh") private Integer xbh;
    @JsonProperty("h_go") private Integer go;
    @JsonProperty("h_ao") private Integer ao;
    @JsonProperty("h_gw_rbi") private Integer gwRbi;

    // --- [누락된 Count 지표: Integer] ---
    @JsonProperty("h_bb") private Integer bb;
    @JsonProperty("h_ibb") private Integer ibb;
    @JsonProperty("h_hbp") private Integer hbp;
    @JsonProperty("h_so") private Integer so;
    @JsonProperty("h_gdp") private Integer gdp;


    // --- [비율/소수점 지표] ---
    @JsonProperty("h_avg") private BigDecimal avg;
    @JsonProperty("h_slg") private BigDecimal slg;
    @JsonProperty("h_obp") private BigDecimal obp;
    @JsonProperty("h_ops") private BigDecimal ops;
    @JsonProperty("h_risp") private BigDecimal risp;
    @JsonProperty("h_ph_ba") private BigDecimal phBa;
    @JsonProperty("h_bb_k") private BigDecimal bbK;
    @JsonProperty("h_isop") private BigDecimal isop;
    @JsonProperty("h_xr") private BigDecimal xr;
    @JsonProperty("h_gpa") private BigDecimal gpa;

    /**
     * DTO를 엔티티로 변환하는 헬퍼 메서드
     */
    public DailyPlayerHitter toEntity(LocalDate baseDate, Player player, Team team) {
        return DailyPlayerHitter.builder()
                .baseDate(baseDate)
                .player(player)
                .team(team)
                .playerName(this.playerName)
                .g(this.g)
                .pa(this.pa)
                .ab(this.ab)
                .r(this.r)
                .h(this.h)
                .b2(this.b2)
                .b3(this.b3)
                .hr(this.hr)
                .tb(this.tb)
                .rbi(this.rbi)
                .sac(this.sac)
                .sf(this.sf)
                .mh(this.mh)
                .xbh(this.xbh)
                .go(this.go)
                .ao(this.ao)
                .gwRbi(this.gwRbi)
                .avg(this.avg)
                .slg(this.slg)
                .obp(this.obp)
                .ops(this.ops)
                .risp(this.risp)
                .phBa(this.phBa)
                .bbK(this.bbK)
                .isop(this.isop)
                .xr(this.xr)
                .gpa(this.gpa)
                .build();
    }
}