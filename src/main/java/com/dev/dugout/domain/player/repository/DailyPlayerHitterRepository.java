package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerHitterId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPlayerHitterRepository extends JpaRepository<DailyPlayerHitter, DailyPlayerHitterId> {
}
