package com.dev.dugout.domain.user.dto;

import com.dev.dugout.domain.team.dto.NewsItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class DashboardResponseDto {
    private String favoriteTeamName; //선호 팀
    private String teamSlogan; //팀 슬로건
    private String bookingUrl; //티켓 예약
    private TeamRankSummaryDto teamRank; // 선호 팀 순위 정보
    private List<PlayerInsightDto> insights; // 선수 성적 예측
    private List<NewsItemDto> news; // 뉴스
}
