package com.dev.dugout.infrastructure.aws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "팀 추천 설문 요청 DTO")
@Getter
@Setter
public class SurveyRequestDto {

    @Schema(description = "야구 입문 연도 (Athena 파티션 프루닝에 사용)", example = "2018")
    private int startYear;

    @Schema(description = """
            6가지 야구 취향 가중치 (각 1~5 정수)
            - q1: 홈런(장타력)
            - q2: 팀 타율(정교함)
            - q3: 선발 방어율(투수력)
            - q4: 불펜(세이브+홀드)
            - q5: OPS(공격 효율)
            - q6: 팀 승률(강팀)
            """,
            example = "{\"q1\":5,\"q2\":2,\"q3\":3,\"q4\":3,\"q5\":4,\"q6\":5}")
    private Map<String, Integer> preferences;

    public String getPreferenceSummary() {
        if (preferences == null || preferences.isEmpty()) {
            return "전반적으로 균형 잡힌 팀을 선호합니다.";
        }

        Map<String, String> traitLabels = Map.of(
                "q1", "호쾌한 홈런과 장타력",
                "q2", "정교하고 높은 팀 타율",
                "q3", "탄탄한 선발 투수진과 낮은 방어율",
                "q4", "뒷문이 강한 불펜진(세이브/홀드)",
                "q5", "출루율과 장타율이 조화로운 OPS",
                "q6", "패배보다 승리가 익숙한 높은 승률"
        );

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
