package com.dev.dugout.domain.player.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

public class GoldenGloveResponseDto {

    @Getter
    @Builder
    public static class LeaderboardResponse {
        private String baseDate;
        private Map<String, List<PlayerPredictionDto>> leaderboardByPosition;
    }

    @Getter
    @Builder
    public static class PlayerPredictionDto {
        private String playerCode;
        private String playerName;
        private String teamName;
        private String position; // "OF" (탭 분류용)
        private String subPosition; // "중견수", "우익수" 등 (UI 출력용)
        private String winProbStr;
        private Integer rank;
        private List<Map<String, Object>> top3Positive;
        private List<Map<String, Object>> top1Negative;
        private String aiExplanation;
    }
}