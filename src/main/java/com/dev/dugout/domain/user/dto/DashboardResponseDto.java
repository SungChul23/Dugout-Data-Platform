package com.dev.dugout.domain.user.dto;

import com.dev.dugout.domain.team.dto.NewsItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "개인화 대시보드 응답 DTO")
@Data
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

    @Schema(description = "사용자 응원 구단명", example = "KIA 타이거즈")
    private String favoriteTeamName;

    @Schema(description = "구단 슬로건", example = "I LOVE KIA TIGERS")
    private String teamSlogan;

    @Schema(description = "구단 공식 티켓 예매 URL")
    private String bookingUrl;

    @Schema(description = "응원 팀 현재 순위 요약")
    private TeamRankSummaryDto teamRank;

    @Schema(description = "즐겨찾기 선수(최대 3명)의 예측 성적 인사이트 목록")
    private List<PlayerInsightDto> insights;

    @Schema(description = "응원 팀 최신 뉴스 목록")
    private List<NewsItemDto> news;
}
