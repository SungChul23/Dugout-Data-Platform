package com.dev.dugout.domain.postseason.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Schema(description = "가을야구 구단 대시보드(구단 개요) 응답 DTO")
@Builder
@Getter
public class PostseasonTeamOverviewResponseDto {

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "팀명", example = "삼성 라이온즈")
    private String teamName;

    @Schema(description = "팀 슬로건", example = "WIN OR WOW! 라이온즈의 자존심")
    private String slogan;

    @Schema(description = "홈구장", example = "대구 삼성 라이온즈 파크")
    private String stadiumName;

    @Schema(description = "한국시리즈 우승 횟수", example = "8")
    private Integer championshipCount;

    @Schema(description = "시즌 연도 (최신 순위 데이터 기준)", example = "2026")
    private Integer season;

    @Schema(description = "정규시즌 순위", example = "2")
    private Integer teamRank;

    @Schema(description = "정규시즌 승률", example = "0.543")
    private BigDecimal winRate;

    @Schema(description = "승", example = "78")
    private Integer wins;

    @Schema(description = "패", example = "65")
    private Integer losses;

    @Schema(description = "무", example = "1")
    private Integer draws;

    @Schema(description = "팀 타율", example = "0.269")
    private BigDecimal teamAvg;

    @Schema(description = "팀 ERA", example = "4.15")
    private BigDecimal teamEra;

    @Schema(description = "팀 홈런", example = "185")
    private Integer teamHr;

    @Schema(description = "홈 경기 성적 (파싱 불가 시 null)")
    private RecordDto homeRecord;

    @Schema(description = "원정 경기 성적 (파싱 불가 시 null)")
    private RecordDto awayRecord;

    @Schema(description = "정규시즌 성적 및 종합 요약 (미등록 시 null)")
    private String summary;

    @Schema(description = "승/패/무 성적")
    @Builder
    @Getter
    public static class RecordDto {
        private Integer wins;
        private Integer losses;
        private Integer draws;
    }
}
