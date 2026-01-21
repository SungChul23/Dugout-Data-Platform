package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 로스터 조회용: 팀 이름으로 조회하되, 특정 포지션(투수)은 제외한 리스트를 가져옴
    // 추후 실제 성적이 들어간다면 최신데이터로 반영해야함
    @Query("SELECT DISTINCT p FROM Player p " +
            "JOIN PredictionResult pr ON p = pr.player " +
            "WHERE p.team.name = :teamName " +
            "AND p.positionType <> '투수' " +
            "AND p.isPredictable = true " +
            "AND p.backNumber IS NOT NULL")
    List<Player> findPredictablePlayersByTeam(@Param("teamName") String teamName);

    // 2상세 분석용: KBO 고유 번호(pcode)로 선수 1명을 조회
    Optional<Player> findByKboPcode(String kboPcode);
}