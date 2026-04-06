package com.dev.dugout.infrastructure.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;
public class GoldenGloveRequestDto {

    @Getter @Setter
    public static class IngestRequest {
        private String baseDate;
        private List<PlayerPrediction> predictions;
    }

    @Getter @Setter
    public static class PlayerPrediction {
        @JsonProperty("h_pcode")
        private String playerCode;

        @JsonProperty("선수명")
        private String playerName;

        @JsonProperty("팀명")
        private String teamName;

        @JsonProperty("pos_eng")
        private String position;

        @JsonProperty("win_prob")
        private Double winProb;

        @JsonProperty("win_prob_str")
        private String winProbStr;

        @JsonProperty("rank")
        private Integer rank;

        @JsonProperty("top3_positive")
        private List<Map<String, Object>> top3Positive;

        @JsonProperty("top1_negative")
        private List<Map<String, Object>> top1Negative;

        @JsonProperty("ai_explanation")
        private String aiExplanation;
    }
}