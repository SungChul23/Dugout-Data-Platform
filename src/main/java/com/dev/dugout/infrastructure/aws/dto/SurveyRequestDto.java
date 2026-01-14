package com.dev.dugout.infrastructure.aws.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class SurveyRequestDto {
    private int startYear; // 시작 연도
    private Map<String, Integer> preferences; // q1:홈런, q2:타율, q3:방어율, q4:세이브/홀드, q5:OPS, q6:승률

    //사용자의 점수(1~5)를 분석하여 Bedrock AI에게 전달할 성향 요약문을 생성합
    public String getPreferenceSummary() {
        if (preferences == null || preferences.isEmpty()) {
            return "전반적으로 균형 잡힌 팀을 선호합니다.";
        }

        // 각 문항이 무엇을 의미하는지 매핑
        Map<String, String> traitLabels = Map.of(
                "q1", "호쾌한 홈런과 장타력",
                "q2", "정교하고 높은 팀 타율",
                "q3", "탄탄한 선발 투수진과 낮은 방어율",
                "q4", "뒷문이 강한 불펜진(세이브/홀드)",
                "q5", "출루율과 장타율이 조화로운 OPS",
                "q6", "패배보다 승리가 익숙한 높은 승률"
        );

        // 4점 이상 준 항목만 추출해서 "강조" 문장 생성
        String highPrefs = preferences.entrySet().stream()
                .filter(entry -> entry.getValue() >= 4)
                .map(entry -> traitLabels.getOrDefault(entry.getKey(), ""))
                .filter(label -> !label.isEmpty())
                .collect(Collectors.joining(", "));

        if (highPrefs.isEmpty()) {
            return "모든 지표에서 기복 없는 안정적인 팀을 선호합니다.";
        }

        return highPrefs + "을(를) 중요하게 생각하는 야구팬입니다.";
    }
}