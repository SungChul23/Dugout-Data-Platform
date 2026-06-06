package com.dev.dugout.domain.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

public class LeaderboardDto {

    @Schema(description = "지표별 TOP 5 리더보드 응답")
    @Getter
    @Builder
    public static class Response {

        @Schema(description = "지표 한글 제목", example = "타율 (AVG)")
        private String title;

        @Schema(description = "지표 식별 키", example = "avg")
        private String metricKey;

        @Schema(description = "지표 단위 (비율 스탯은 빈 문자열)", example = "개")
        private String unit;

        @Schema(description = "TOP 5 순위 목록")
        private List<RankItem> ranks;
    }

    @Schema(description = "리더보드 개별 순위 항목")
    @Getter
    @Builder
    public static class RankItem {

        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "선수명", example = "김도영")
        private String playerName;

        @Schema(description = "소속 구단명", example = "KIA 타이거즈")
        private String teamName;

        @Schema(description = "지표 표시값 (소수점 포맷 적용)", example = "0.358")
        private String displayValue;
    }

    @Getter
    public static class MetricConfig<T> {
        private final String title;
        private final String key;
        private final String unit;
        private final boolean isDesc;
        private final int precision;
        private final boolean isRateStat;
        private final Function<T, Double> extractor;

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
