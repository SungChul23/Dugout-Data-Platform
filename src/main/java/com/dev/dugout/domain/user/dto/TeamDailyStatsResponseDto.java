package com.dev.dugout.domain.user.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TeamDailyStatsResponseDto {
    private String teamName;
    private LocalDate baseDate;
    private List<PlayerStatDto> hitters;
    private List<PlayerStatDto> pitchers;

    @Getter
    @Builder
    public static class PlayerStatDto {
        private String playerId; // Player의 kbo_pcode 타입에 맞게 String 조정
        private String playerName;
        // Integer와 BigDecimal을 모두 담기 위해 Number 타입 사용
        private Map<String, Number> stats;
    }
}