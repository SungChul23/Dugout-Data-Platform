package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.TeamRankingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTeamRankingRepository extends JpaRepository<DailyTeamRanking, TeamRankingId> {

    //대시보드에서 날짜별로 순위를 나열할 때 가장 많이 사용


    //특정 날짜의 전체 팀 순위 조회
    List<DailyTeamRanking> findByRankingDateOrderByRankAsc(LocalDate rankingDate);

    //특정 팀의 기간별 순위 변동 추이 조회
    List<DailyTeamRanking> findByTeamIdAndRankingDateBetweenOrderByRankingDateAsc(
            Long teamId, LocalDate startDate, LocalDate endDate);

    //가장 최근 기록된 날짜의 데이터 존재 여부 확인
    boolean existsByRankingDate(LocalDate rankingDate);
}