package com.dev.dugout.domain.player.repository;

import com.dev.dugout.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 로스터 조회용: 팀 이름으로 조회하되, 특정 포지션(투수)은 제외한 리스트를 가져옴
    List<Player> findAllByTeamNameAndPositionTypeNot(String teamName, String positionType);

    // 2상세 분석용: KBO 고유 번호(pcode)로 선수 1명을 조회
    Optional<Player> findByKboPcode(String kboPcode);
}