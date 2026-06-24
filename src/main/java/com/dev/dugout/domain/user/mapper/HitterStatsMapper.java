package com.dev.dugout.domain.user.mapper;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.user.dto.TeamDailyStatsResponseDto;

import java.util.HashMap;
import java.util.Map;

/**
 * 타자 Entity → DTO 변환 매퍼.
 * DashboardService에서 추출된 순수 변환 로직으로, 외부 의존성 없이 독립 테스트 가능.
 */
public class HitterStatsMapper {

    private HitterStatsMapper() {
        // 유틸리티 클래스
    }

    public static TeamDailyStatsResponseDto.PlayerStatDto toDto(DailyPlayerHitter h) {
        Map<String, Number> stats = new HashMap<>();

        // Count 지표
        stats.put("h_g", h.getG());
        stats.put("h_pa", h.getPa());
        stats.put("h_ab", h.getAb());
        stats.put("h_r", h.getR());
        stats.put("h_h", h.getH());
        stats.put("h_2b", h.getB2());
        stats.put("h_3b", h.getB3());
        stats.put("h_hr", h.getHr());
        stats.put("h_tb", h.getTb());
        stats.put("h_rbi", h.getRbi());
        stats.put("h_bb", h.getBb());
        stats.put("h_ibb", h.getIbb());
        stats.put("h_hbp", h.getHbp());
        stats.put("h_so", h.getSo());
        stats.put("h_gdp", h.getGdp());
        stats.put("h_sac", h.getSac());
        stats.put("h_sf", h.getSf());
        stats.put("h_mh", h.getMh());
        stats.put("h_xbh", h.getXbh());
        stats.put("h_go", h.getGo());
        stats.put("h_ao", h.getAo());
        stats.put("h_gw_rbi", h.getGwRbi());

        // Ratio 지표
        stats.put("h_avg", h.getAvg());
        stats.put("h_slg", h.getSlg());
        stats.put("h_obp", h.getObp());
        stats.put("h_ops", h.getOps());
        stats.put("h_risp", h.getRisp());
        stats.put("h_ph_ba", h.getPhBa());
        stats.put("h_bb_k", h.getBbK());
        stats.put("h_isop", h.getIsop());
        stats.put("h_xr", h.getXr());
        stats.put("h_gpa", h.getGpa());

        return TeamDailyStatsResponseDto.PlayerStatDto.builder()
                .playerId(h.getPlayer().getKboPcode())
                .playerName(h.getPlayerName())
                .stats(stats)
                .build();
    }
}
