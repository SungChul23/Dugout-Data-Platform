package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerHitterId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyPlayerHitterRepository extends JpaRepository<DailyPlayerHitter, DailyPlayerHitterId> {

    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);

}
