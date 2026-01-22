package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 로스터 조회용
    // 타자 명단 조회
    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN PredictionResult pr ON p = pr.player " +
            "WHERE p.team.name = :teamName " +
            "AND p.positionType <> '투수' " + // '투수'가 아닌 경우
            "AND p.isPredictable = true " +  // 예측 데이터가 준비된 상태
            "AND p.backNumber IS NOT NULL " +
            "ORDER BY CAST(p.backNumber AS integer) ASC")
    List<Player> findPredictableHitters(@Param("teamName") String teamName);

    // 투수 명단 조회
    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN PredictionResult pr ON p = pr.player " +
            "WHERE p.team.name = :teamName " +
            "AND p.positionType = '투수' " +  // '투수'인 경우
            "AND p.isPredictable = true " +  // 예측 데이터가 준비된 상태 (이제 투수도 true여야 함)
            "AND p.backNumber IS NOT NULL " +
            "ORDER BY CAST(p.backNumber AS integer) ASC")
    List<Player> findPredictablePitchers(@Param("teamName") String teamName);

    // 2상세 분석용: KBO 고유 번호(pcode)로 선수 1명을 조회
    Optional<Player> findByKboPcode(String kboPcode);
}