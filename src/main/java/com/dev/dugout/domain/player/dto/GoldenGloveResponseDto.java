package com.dev.dugout.domain.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

public class GoldenGloveResponseDto {

    @Schema(description = "골든글러브 예측 리더보드 응답")
    @Getter
    @Builder
    public static class LeaderboardResponse {

        @Schema(description = "예측 기준일 (YYYY-MM-DD)", example = "2026-06-06")
        private String baseDate;

        @Schema(description = "포지션별 예측 선수 목록 (key: 포지션 코드, value: 예측 선수 리스트)")
        private Map<String, List<PlayerPredictionDto>> leaderboardByPosition;
    }

    @Schema(description = "포지션별 골든글러브 후보 선수 예측 정보")
    @Getter
    @Builder
    public static class PlayerPredictionDto {

        @Schema(description = "KBO 선수 코드", example = "79047")
        private String playerCode;

        @Schema(description = "선수명", example = "김도영")
        private String playerName;

        @Schema(description = "소속 구단명", example = "KIA 타이거즈")
        private String teamName;

        @Schema(description = "탭 분류용 포지션 (일반 포지션 또는 OF)", example = "3B")
        private String position;

        @Schema(description = "외야수 세부 포지션 (UI 출력용, 외야수만 해당)", example = "중견수")
        private String subPosition;

        @Schema(description = "수상 확률 문자열 표현", example = "87.3%")
        private String winProbStr;

        @Schema(description = "포지션 내 순위", example = "1")
        private Integer rank;

        @Schema(description = "수상에 긍정적으로 기여한 상위 3개 지표 (SHAP 기반)", example = "[{\"feature\": \"타율\", \"value\": 0.358}]")
        private List<Map<String, Object>> top3Positive;

        @Schema(description = "수상에 부정적으로 작용한 상위 1개 지표 (SHAP 기반)", example = "[{\"feature\": \"수비율\", \"value\": -0.023}]")
        private List<Map<String, Object>> top1Negative;

        @Schema(description = "Amazon Bedrock AI 생성 수상 근거 설명")
        private String aiExplanation;
    }
}
