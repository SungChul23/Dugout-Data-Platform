package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HitterRepository extends JpaRepository<DailyPlayerHitter, Long> {
    @Query("SELECT MAX(h.baseDate) FROM DailyPlayerHitter h")
    LocalDate findMaxBaseDate();

    // 팀 정보 모두 가져와야함
    @Query("SELECT h FROM DailyPlayerHitter h JOIN FETCH h.team WHERE h.baseDate = :baseDate")
    List<DailyPlayerHitter> findByBaseDateWithTeam(@Param("baseDate") LocalDate baseDate);

    List<DailyPlayerHitter> findByBaseDate(LocalDate baseDate);
}