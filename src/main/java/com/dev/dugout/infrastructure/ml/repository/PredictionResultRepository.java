package com.dev.dugout.infrastructure.ml.repository;

import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {

     //특정 선수의 가장 최근 예측 결과를 조회
     //대시보드 카드에 실시간 데이터(타율, 홈런 등)와 신뢰도를 뿌려줄 때 사용
    Optional<PredictionResult> findTopByPlayerOrderByPredictedAtDesc(Player player);
}