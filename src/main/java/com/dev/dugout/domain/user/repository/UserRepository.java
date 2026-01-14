package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname (String nickname);

    // 로그인을 위해 이메일(loginId)로 사용자를 찾는 메서드
    Optional<User> findByLoginId(String loginId);

    // @Param을 사용해 loginId로 유저와 팀 정보를 한 번에 조회
    @Query("select u from User u join fetch u.favoriteTeam where u.loginId = :loginId")
    Optional<User> findByLoginIdWithTeam(@Param("loginId") String loginId);

}
