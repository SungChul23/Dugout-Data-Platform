package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcherId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPlayerPitcherRepository extends JpaRepository<DailyPlayerPitcher, DailyPlayerPitcherId> {
}
