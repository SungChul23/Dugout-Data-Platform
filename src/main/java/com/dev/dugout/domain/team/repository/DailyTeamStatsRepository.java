package com.dev.dugout.domain.team.repository;


import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.entity.TeamStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTeamStatsRepository extends JpaRepository<DailyTeamStats, TeamStatsId> {

    // 특정 날짜의 특정 팀 성적 존재 여부 확인
    boolean existsByBaseDateAndTeamId(LocalDate baseDate, Long teamId);

    // 특정 날짜의 팀 성적 삭제 (재입고 시 사용)
    void deleteByBaseDate(LocalDate baseDate);

    // 가장 최근 성적 조회
    Optional<DailyTeamStats> findFirstByTeamIdOrderByBaseDateDesc(Long teamId);

    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);
}