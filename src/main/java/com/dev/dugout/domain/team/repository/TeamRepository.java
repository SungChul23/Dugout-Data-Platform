package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

//팀별 티켓 예매 사이트 db로 이전 (예매처 변경을 대비)
public interface TeamRepository extends JpaRepository<Team,Long> {

    List<Team> findAllByOrderByNameAsc();
}
