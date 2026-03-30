package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.TeamRankingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTeamRankingRepository extends JpaRepository<DailyTeamRanking, TeamRankingId> {

    //특정 날짜의 전체 팀 순위 조회 (순위 순으로 정렬)
    List<DailyTeamRanking> findByBaseDateOrderByRankAsc(LocalDate baseDate);

    //특정 팀의 기간별 순위 변동 추이 조회 (날짜 순으로 정렬)
    List<DailyTeamRanking> findByTeamIdAndBaseDateBetweenOrderByBaseDateAsc(
            Long teamId, LocalDate startDate, LocalDate endDate);

    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);

}