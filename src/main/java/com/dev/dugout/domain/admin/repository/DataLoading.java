package com.dev.dugout.domain.admin.repository;

import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// CSV 파일을 읽어 DB에 넣을 때, 이미 데이터가 있는지 확인하고 덮어쓰기 위해 필요
public interface DataLoading extends JpaRepository<PredictionResult,Long> {

    Optional<PredictionResult> findByPlayerAndTargetSeason(Player player, String targetSeason);
}
