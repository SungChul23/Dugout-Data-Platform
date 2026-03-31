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
        private final String unit;
        private final boolean isDesc; // true: 내림차순(높은게 1등), false: 오름차순(낮은게 1등) 중요함 햇갈리지 말기
        private final int precision;  // 소수점 자릿수 (0이면 정수)
        private final Function<T, Double> extractor; // 엔티티에서 값을 꺼내는 함수

        public MetricConfig(String title, String key, String unit, boolean isDesc, int precision, Function<T, Number> extractor) {
            this.title = title;
            this.key = key;
            this.unit = unit;
            this.isDesc = isDesc;
            this.precision = precision;
            // 안전한 비교를 위해 Double로 통일
            this.extractor = entity -> {
                Number val = extractor.apply(entity);
                return val != null ? val.doubleValue() : (isDesc ? -999.0 : 999.0);
            };
        }
    }
}