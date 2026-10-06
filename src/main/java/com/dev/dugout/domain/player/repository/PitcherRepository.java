package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PitcherRepository extends JpaRepository<DailyPlayerPitcher, Long> {
    @Query("SELECT MAX(p.baseDate) FROM DailyPlayerPitcher p")
    LocalDate findMaxBaseDate();

    // 팀 정보 모두 가져와야함
    @Query("SELECT p FROM DailyPlayerPitcher p JOIN FETCH p.team WHERE p.baseDate = :baseDate")
    List<DailyPlayerPitcher> findByBaseDateWithTeam(@Param("baseDate") LocalDate baseDate);

    List<DailyPlayerPitcher> findByBaseDate(LocalDate baseDate);

    // 특정 날짜 이전(포함)의 가장 최근 데이터 날짜 (최근 N일 성적 계산의 기준일)
    @Query("SELECT MAX(p.baseDate) FROM DailyPlayerPitcher p WHERE p.baseDate <= :date")
    LocalDate findMaxBaseDateOnOrBefore(@Param("date") LocalDate date);

    // 선수 ID로 스냅샷끼리 매칭해야 하므로 player까지 함께 가져온다
    @Query("SELECT p FROM DailyPlayerPitcher p JOIN FETCH p.team JOIN FETCH p.player WHERE p.baseDate = :baseDate")
    List<DailyPlayerPitcher> findByBaseDateWithTeamAndPlayer(@Param("baseDate") LocalDate baseDate);
}