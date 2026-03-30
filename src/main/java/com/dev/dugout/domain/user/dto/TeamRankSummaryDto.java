package com.dev.dugout.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
//사용자 대시보드에서의 팀 성적 추이
public class TeamRankSummaryDto {
    private Long teamId;
    private Integer rank;
    private String wdl; // "승-무-패" 포맷
    private String winRate; // "0.556" 포맷
}
