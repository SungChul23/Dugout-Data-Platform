package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.RefreshToken;
import com.dev.dugout.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//누구의 토큰이냐
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
}
