package com.dev.dugout.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardResponseDto {
    private String favoriteTeamName;
    private String teamSlogan;
    private List<PlayerInsightDto> insights; // 항상 사이즈 3 유지
}
