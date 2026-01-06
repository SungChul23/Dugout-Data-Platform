package com.dev.dugout.domain.user.service;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequestDto requestDto) {
        // 1. 비밀번호 암호화 (BCrypt)
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 2. 엔티티 빌더를 사용하여 생성 (User 필드 순서 준수)
        // 성철님의 User 엔티티 구조에 맞게 매핑
        User user = User.builder()
                .loginId(requestDto.getEmail()) // 이메일을 loginId로 저장
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .favoriteTeam(requestDto.getFavoriteTeam())
                .build();

        userRepository.save(user);
    }

    // 닉네임 중복 체크
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    //로그인 로직
    @Transactional(readOnly = true)
    public boolean login(LoginRequestDto loginDto) {
        // 1. DB에서 이메일(loginId)로 사용자 조회
        User user = userRepository.findByLoginId(loginDto.getEmail())
                .orElse(null);

        if (user == null) {
            return false; // 사용자가 없으면 로그인 실패
        }

        // 2. 암호화된 비밀번호와 입력된 평문 비밀번호 비교
        // matches(평문, 암호화된 비밀번호) 순서가 중요합니다.
        return passwordEncoder.matches(loginDto.getPassword(), user.getPassword());
    }

    public String getNicknameIfValid(LoginRequestDto loginDto) {
        User user = userRepository.findByLoginId(loginDto.getEmail()).orElse(null);

        // passwordEncoder.matches()로 암호 비교 (지난번 직접 생성한 객체 사용)
        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return user.getNickname();
        }
        return null;
    }

}