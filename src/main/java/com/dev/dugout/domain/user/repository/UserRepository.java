package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.User;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname (String nickname);

    // 로그인을 위해 이메일(loginId)로 사용자를 찾는 메서드
    Optional<User> findByLoginId(String loginId);
}
