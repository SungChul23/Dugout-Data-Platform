package com.dev.dugout.domain.user.mapper;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.user.dto.TeamDailyStatsResponseDto;

import java.util.HashMap;
import java.util.Map;

/**
 * 투수 Entity → DTO 변환 매퍼.
 * DashboardService에서 추출된 순수 변환 로직으로, 외부 의존성 없이 독립 테스트 가능.
 */
public class PitcherStatsMapper {

    private PitcherStatsMapper() {
        // 유틸리티 클래스
    }

    public static TeamDailyStatsResponseDto.PlayerStatDto toDto(DailyPlayerPitcher p) {
        Map<String, Number> stats = new HashMap<>();

        // Basic 1
        stats.put("p_era", p.getEra());
        stats.put("p_g", p.getG());
        stats.put("p_w", p.getW());
        stats.put("p_l", p.getL());
        stats.put("p_sv", p.getSv());
        stats.put("p_hld", p.getHld());
        stats.put("p_wpct", p.getWpct());
        stats.put("p_ip", p.getIp());
        stats.put("p_h", p.getH());
        stats.put("p_hr", p.getHr());
        stats.put("p_bb", p.getBb());
        stats.put("p_hbp", p.getHbp());
        stats.put("p_so", p.getSo());
        stats.put("p_r", p.getR());
        stats.put("p_er", p.getEr());
        stats.put("p_whip", p.getWhip());

        // Basic 2
        stats.put("p_cg", p.getCg());
        stats.put("p_sho", p.getSho());
        stats.put("p_qs", p.getQs());
        stats.put("p_bsv", p.getBsv());
        stats.put("p_tbf", p.getTbf());
        stats.put("p_np", p.getNp());
        stats.put("p_avg", p.getAvg());
        stats.put("p_2b", p.getB2());
        stats.put("p_3b", p.getB3());
        stats.put("p_sac", p.getSac());
        stats.put("p_sf", p.getSf());
        stats.put("p_ibb", p.getIbb());
        stats.put("p_wp", p.getWp());
        stats.put("p_bk", p.getBk());

        // Detail 1
        stats.put("p_gs", p.getGs());
        stats.put("p_wgs", p.getWgs());
        stats.put("p_wgr", p.getWgr());
        stats.put("p_gf", p.getGf());
        stats.put("p_svo", p.getSvo());
        stats.put("p_ts", p.getTs());
        stats.put("p_gdp", p.getGdp());
        stats.put("p_go", p.getGo());
        stats.put("p_ao", p.getAo());
        stats.put("p_go_ao", p.getGoAo());

        return TeamDailyStatsResponseDto.PlayerStatDto.builder()
                .playerId(p.getPlayer().getKboPcode())
                .playerName(p.getPlayerName())
                .stats(stats)
                .build();
    }
}
