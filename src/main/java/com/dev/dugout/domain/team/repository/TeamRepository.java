package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

//팀별 티켓 예매 사이트 db로 이전 (예매처 변경을 대비)
public interface TeamRepository extends JpaRepository<Team,Long> {

    List<Team> findAllByOrderByNameAsc();

    //회원가입시 프런트에서 받은 구단 명 -> 구단 고유 코드로 치환
    Optional<Team> findByName(String name);
}
