package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerHitterId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPlayerHitterRepository extends JpaRepository<DailyPlayerHitter, DailyPlayerHitterId> {

    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);

    // 개인화 대시보드 기능에서 타자 성적 가져오기 (사용자가 선호 하는 팀)
    @Query("SELECT MAX(h.baseDate) FROM DailyPlayerHitter h")
    Optional<LocalDate> findMaxBaseDate();

    @Query("SELECT h FROM DailyPlayerHitter h JOIN FETCH h.player WHERE h.baseDate = :baseDate AND h.team.id = :teamId")
    List<DailyPlayerHitter> findByBaseDateAndTeamId(@Param("baseDate") LocalDate baseDate, @Param("teamId") Long teamId);
}
