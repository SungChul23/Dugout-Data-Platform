package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


//회원가입시 프런트에서 받은 구단 명 -> 구단 고유 코드로 치환
public interface TeamRepository extends JpaRepository<Team,Long> {
    Optional<Team> findByName(String name);
}
