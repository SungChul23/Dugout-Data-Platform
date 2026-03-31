package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface HitterRepository extends JpaRepository<DailyPlayerHitter, Long> {
    @Query("SELECT MAX(h.baseDate) FROM DailyPlayerHitter h")
    LocalDate findMaxBaseDate();

    List<DailyPlayerHitter> findByBaseDate(LocalDate baseDate);
}