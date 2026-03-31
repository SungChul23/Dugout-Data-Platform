package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.TeamRankingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTeamRankingRepository extends JpaRepository<DailyTeamRanking, TeamRankingId> {

    //특정 날짜의 전체 팀 순위 조회 (순위 순으로 정렬)
    List<DailyTeamRanking> findAllByBaseDateOrderByRankAsc(LocalDate baseDate);

    //특정 팀의 기간별 순위 변동 추이 조회 (날짜 순으로 정렬)
    List<DailyTeamRanking> findByTeamIdAndBaseDateBetweenOrderByBaseDateAsc(
            Long teamId, LocalDate startDate, LocalDate endDate);

    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);

    @Query("SELECT MAX(r.baseDate) FROM DailyTeamRanking r")
    Optional<LocalDate> findMaxBaseDate();

    // 날짜 조건 없이 DB에 있는 전체 순위 데이터를 날짜 오름차순으로 싹 다 가져옴 (팀 순위 변동 그래프 전용)
    @Query("SELECT r FROM DailyTeamRanking r JOIN FETCH r.team ORDER BY r.baseDate ASC")
    List<DailyTeamRanking> findAllRankingsWithTeam();

    // 특정 날짜의 특정 팀 순위 조회 (사용자 대시보드 전용 로직)
    Optional<DailyTeamRanking> findByBaseDateAndTeamId(LocalDate baseDate, Long teamId);

}