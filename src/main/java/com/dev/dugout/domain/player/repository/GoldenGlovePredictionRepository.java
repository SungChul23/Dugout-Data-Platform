package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.GoldenGlovePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoldenGlovePredictionRepository extends JpaRepository<GoldenGlovePrediction, Long> {

    // 멱등성 보장을 위해 기존 날짜 데이터 삭제용
    // 데이프라인 재실행 시 데이터가 중복해서 쌓이는 것을 방지하기 위함
    void deleteByBaseDate(LocalDate baseDate);


    @Query("SELECT MAX(g.baseDate) FROM GoldenGlovePrediction g")
    Optional<LocalDate> findLatestBaseDate();

    // Player 엔티티와 조인하여 g(예측 데이터)와 p.subPositionType(상세 포지션)을 함께 가져옴

    @Query("SELECT g, p.subPositionType FROM GoldenGlovePrediction g " +
            "LEFT JOIN Player p ON g.playerCode = p.kboPcode " +
            "WHERE g.baseDate = :baseDate " +
            "AND (" +
            "  (g.position != 'OF' AND g.rank <= 3) " +
            "  OR " +
            "  (g.position = 'OF' AND g.rank <= 6)" +
            ") " +
            "ORDER BY g.position ASC, g.rank ASC")
    List<Object[]> findTopContendersWithSubPosition(@Param("baseDate") LocalDate baseDate);



}