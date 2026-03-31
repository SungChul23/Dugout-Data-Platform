package com.dev.dugout.domain.player.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.function.Function;

public class LeaderboardDto {

    @Getter
    @Builder
    public static class Response {
        private String title;
        private String metricKey;
        private String unit;
        private List<RankItem> ranks;
    }

    @Getter
    @Builder
    public static class RankItem {
        private int rank;
        private String playerName;
        private String teamName;
        private String displayValue;
    }

    // 지표별 정렬 및 추출 규칙을 정의하는 마법의 클래스
    @Getter
    public static class MetricConfig<T> {
        private final String title;
        private final String key;
        private final String unit; // ~개 (ex: 홈런)
        private final boolean isDesc; // 내림차순 or 오름차순
        private final int precision;
        private final boolean isRateStat; // (비율 스탯 여부)
        private final Function<T, Double> extractor;

        // 생성자에 isRateStat 파라미터 추가
        public MetricConfig(String title, String key, String unit, boolean isDesc, int precision, boolean isRateStat, Function<T, Number> extractor) {
            this.title = title;
            this.key = key;
            this.unit = unit;
            this.isDesc = isDesc;
            this.precision = precision;
            this.isRateStat = isRateStat;
            this.extractor = entity -> {
                Number val = extractor.apply(entity);
                return val != null ? val.doubleValue() : (isDesc ? -999.0 : 999.0);
            };
        }
    }
}