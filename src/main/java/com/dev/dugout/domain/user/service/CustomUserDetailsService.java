package com.dev.dugout.domain.user.service;


import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.global.config.UserPrincipal; // 이미 만드신 클래스
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

//진짜 우리 회원이 맞을까요?
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // 1. DB에서 loginId(이메일 등)로 유저를 찾습니다.
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디를 가진 유저를 찾을 수 없습니다: " + loginId));

        // 2. 찾은 유저 엔티티를 UserPrincipal(UserDetails 구현체)로 감싸서 반환합니다.
        return new UserPrincipal(user);
    }
}
