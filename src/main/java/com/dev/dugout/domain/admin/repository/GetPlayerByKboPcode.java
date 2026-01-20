package com.dev.dugout.domain.admin.repository;

import com.dev.dugout.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GetPlayerByKboPcode extends JpaRepository<Player, Long> {
    // CSV의 첫 번째 컬럼인 kbo_pcode로 선수를 조회하는 메서드
    Optional<Player> findByKboPcode(String KboPcode);
}
