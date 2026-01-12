package com.dev.dugout.domain.user.service;

import com.dev.dugout.domain.user.dto.*;
import com.dev.dugout.domain.user.entity.ForbiddenWord;
import com.dev.dugout.domain.user.entity.RefreshToken;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.ForbiddenWordRepository;
import com.dev.dugout.domain.user.repository.RefreshTokenRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;

    // 금칙어를 저장할 메모리 캐시 (멀티쓰레드 환경을 고려해 CopyOnWriteArrayList 사용 가능)
    private List<String> forbiddenWordCache;

    @PostConstruct //Bean이 “쓸 준비 완료” 되자마자 한 번 실행되는 초기화 훅
    public void initForbiddenWords() {
        List<ForbiddenWord> allWords = forbiddenWordRepository.findAll();
        this.forbiddenWordCache = allWords.stream()
                .map(ForbiddenWord::getWord)
                .toList();
        log.info(">>>> [Cache] 금칙어 {}건이 메모리에 로드되었습니다.", forbiddenWordCache.size());
    }

    //금칙어 리스트를 강제로 갱신하고 싶을 때 호출하는 메서드 (나중에 괸라자 전용 페이지에서 사용 할 예정)
    public void refreshForbiddenWords() {
        initForbiddenWords();
    }

    @Transactional
    public LoginResponseDto signup(SignupRequestDto requestDto) {
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        User user = User.builder()
                .loginId(requestDto.getEmail())
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .favoriteTeam(requestDto.getFavoriteTeam())
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponseDto checkNicknameAvailability(String nickname) {
        // 1. 기본 유효성 및 길이 검사
        if (nickname == null || nickname.trim().isEmpty()) {
            return new NicknameCheckResponseDto(false, "닉네임을 입력해주세요.");
        }

        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 10) {
            return new NicknameCheckResponseDto(false, "닉네임은 2자 이상 10자 이하로 입력해주세요.");
        }

        // 2. 숫자 및 특수문자 차단 (정규표현식)
        // ^[a-zA-Z가-힣]*$ : 시작부터 끝까지 영문 대소문자와 한글만 허용함
        if (!trimmedNickname.matches("^[a-zA-Z가-힣]*$")) {
            return new NicknameCheckResponseDto(false, "닉네임은 한글과 영문만 가능하며, 숫자나 특수문자는 사용할 수 없습니다.");
        }

        // 3. 금칙어 필터링을 위한 전처리 (공백 제거)
        String cleanNickname = trimmedNickname.replaceAll("\\s", "");

        // 4. 금칙어/욕설 필터링 (메모리 캐시 사용)
        for (String forbiddenWord : forbiddenWordCache) {
            if (cleanNickname.contains(forbiddenWord)) {
                return new NicknameCheckResponseDto(false, "사용할 수 없는 단어가 포함되어 있습니다.");
            }
        }

        // 5. 중복 확인
        boolean exists = userRepository.existsByNickname(trimmedNickname);
        if (exists) {
            return new NicknameCheckResponseDto(false, "이미 사용 중인 닉네임입니다.");
        }

        return new NicknameCheckResponseDto(true, "사용 가능한 닉네임입니다.");
    }

    @Transactional
    public LoginResponseDto getLoginUserInfo(LoginRequestDto loginDto) {
        User user = userRepository.findByLoginId(loginDto.getEmail()).orElse(null);

        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return issueTokens(user);
        }
        return null;
    }

    private LoginResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        t -> t.updateToken(refreshToken),
                        () -> {
                            RefreshToken newToken = RefreshToken.builder()
                                    .user(user)
                                    .token(refreshToken)
                                    .build();
                            refreshTokenRepository.save(newToken);
                        }
                );

        return new LoginResponseDto(accessToken, refreshToken, user.getNickname(), user.getFavoriteTeam());
    }
}