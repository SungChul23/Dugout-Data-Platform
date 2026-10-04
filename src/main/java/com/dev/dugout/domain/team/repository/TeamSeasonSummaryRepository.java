package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.TeamSeasonSummary;
import com.dev.dugout.domain.team.entity.TeamSeasonSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamSeasonSummaryRepository extends JpaRepository<TeamSeasonSummary, TeamSeasonSummaryId> {

    // 특정 팀의 특정 시즌 종합 요약 조회
    Optional<TeamSeasonSummary> findByTeamIdAndSeason(Long teamId, Long season);
}
