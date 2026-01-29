package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.FaMarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaMarketRepository extends JpaRepository<FaMarket, Long> {
    //연도 + 팀id + fa대상자 조건으로 가져오자
    List<FaMarket> findByYearAndTeamIdAndIsFaTargetTrue(Integer year, Integer teamId);
}
