package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface PitcherRepository extends JpaRepository<DailyPlayerPitcher, Long> {
    @Query("SELECT MAX(p.baseDate) FROM DailyPlayerPitcher p")
    LocalDate findMaxBaseDate();

    List<DailyPlayerPitcher> findByBaseDate(LocalDate baseDate);
}