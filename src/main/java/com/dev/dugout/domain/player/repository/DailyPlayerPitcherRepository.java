package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPlayerPitcherRepository extends JpaRepository<DailyPlayerPitcher, DailyPlayerPitcherId> {
    //특정 날짜의 데이터 존재 여부 확인
    boolean existsByBaseDate(LocalDate baseDate);


    // 개인화 대시보드 기능에서 투수 성적 가져오기 (사용자가 선호 하는 팀)
    @Query("SELECT MAX(p.baseDate) FROM DailyPlayerPitcher p")
    Optional<LocalDate> findMaxBaseDate();

    @Query("SELECT p FROM DailyPlayerPitcher p JOIN FETCH p.player WHERE p.baseDate = :baseDate AND p.team.id = :teamId")
    List<DailyPlayerPitcher> findByBaseDateAndTeamId(@Param("baseDate") LocalDate baseDate, @Param("teamId") Long teamId);
}
