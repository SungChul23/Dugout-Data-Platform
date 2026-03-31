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
}