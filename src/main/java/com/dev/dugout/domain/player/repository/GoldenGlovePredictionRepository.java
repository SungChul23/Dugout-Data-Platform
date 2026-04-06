package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.GoldenGlovePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface GoldenGlovePredictionRepository extends JpaRepository<GoldenGlovePrediction, Long> {

    // 멱등성 보장을 위해 기존 날짜 데이터 삭제용
    // 데이프라인 재실행 시 데이터가 중복해서 쌓이는 것을 방지하기 위함
    void deleteByBaseDate(LocalDate baseDate);
}