package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // 특정 월의 시작일과 종료일 사이의 모든 경기를 가져옴(feat.JPQL)
    @Query("SELECT g FROM Game g JOIN FETCH g.homeTeam JOIN FETCH g.awayTeam " +
            "WHERE g.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY g.gameDate ASC, g.gameTime ASC")
    List<Game> findMonthlySchedule(LocalDate startDate, LocalDate endDate);


    ///////////////[경기 결과 및 상태] ///////////////

    // 특정 날짜의 모든 경기 조회 (최적화 방어 로직용)
    List<Game> findAllByGameDate(LocalDate gameDate);

    // 날짜 + 홈팀 + 원정팀으로 특정 경기 찾기 (업데이트용)
    @Query("SELECT g FROM Game g WHERE g.gameDate = :gameDate AND g.homeTeam.id = :homeTeamId AND g.awayTeam.id = :awayTeamId")
    Optional<Game> findByGameDateAndHomeTeamIdAndAwayTeamId(LocalDate gameDate, Long homeTeamId, Long awayTeamId);
}
