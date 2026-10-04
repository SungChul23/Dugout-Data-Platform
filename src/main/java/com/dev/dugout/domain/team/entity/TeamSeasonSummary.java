package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;

//구단별 시즌 종합 요약 (수동 입력 데이터)
@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(TeamSeasonSummaryId.class)
@Table(name = "team_season_summary")
public class TeamSeasonSummary {

    @Id
    @Column(name = "team_id")
    private Long teamId;

    @Id
    private Long season; // 시즌 연도 (예: 2026)

    @Column(columnDefinition = "TEXT")
    private String summary;
}
