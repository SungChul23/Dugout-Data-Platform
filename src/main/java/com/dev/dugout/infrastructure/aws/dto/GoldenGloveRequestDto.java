package com.dev.dugout.infrastructure.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class GoldenGloveRequestDto {

    @Schema(description = "골든글러브 예측 데이터 입고 요청 (Lambda → Spring)")
    @Getter
    @Setter
    public static class IngestRequest {

        @Schema(description = "예측 기준일 (YYYY-MM-DD)", example = "2026-06-06")
        private String baseDate;

        @Schema(description = "포지션별 선수 예측 목록")
        private List<PlayerPrediction> predictions;
    }

    @Schema(description = "선수별 골든글러브 수상 예측 정보")
    @Getter
    @Setter
    public static class PlayerPrediction {

        @Schema(description = "KBO 선수 코드", example = "79047")
        @JsonProperty("h_pcode")
        private String playerCode;

        @Schema(description = "선수명", example = "김도영")
        @JsonProperty("선수명")
        private String playerName;

        @Schema(description = "소속 구단명", example = "KIA 타이거즈")
        @JsonProperty("팀명")
        private String teamName;

        @Schema(description = "포지션 코드 (영문)", example = "3B")
        @JsonProperty("pos_eng")
        private String position;

        @Schema(description = "수상 확률 (0~1)", example = "0.873")
        @JsonProperty("win_prob")
        private Double winProb;

        @Schema(description = "수상 확률 문자열", example = "87.3%")
        @JsonProperty("win_prob_str")
        private String winProbStr;

        @Schema(description = "포지션 내 순위", example = "1")
        @JsonProperty("rank")
        private Integer rank;

        @Schema(description = "긍정 기여 TOP 3 지표 (SHAP 값)")
        @JsonProperty("top3_positive")
        private List<Map<String, Object>> top3Positive;

        @Schema(description = "부정 기여 TOP 1 지표 (SHAP 값)")
        @JsonProperty("top1_negative")
        private List<Map<String, Object>> top1Negative;

        @Schema(description = "Bedrock AI 생성 수상 근거 설명")
        @JsonProperty("ai_explanation")
        private String aiExplanation;
    }
}
