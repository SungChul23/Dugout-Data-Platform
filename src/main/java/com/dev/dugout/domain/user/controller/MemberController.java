package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.dto.NicknameCheckResponseDto;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import com.dev.dugout.domain.user.service.MemberService;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<LoginResponseDto> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        LoginResponseDto responseDto = memberService.signup(requestDto);

        // 가입 직후 자동 로그인을 위한 쿠키 생성
        ResponseCookie accessCookie = jwtTokenProvider.createAccessTokenCookie(requestDto.getEmail());
        ResponseCookie refreshCookie = jwtTokenProvider.createRefreshTokenCookie(requestDto.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseDto);
    }

    // 닉네임 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<NicknameCheckResponseDto> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(memberService.checkNicknameAvailability(nickname));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginDto) {
        LoginResponseDto responseDto = memberService.getLoginUserInfo(loginDto);

        if (responseDto != null) {
            // [개선] Provider에게 쿠키 생성을 맡김 (일관성 유지)
            ResponseCookie accessCookie = jwtTokenProvider.createAccessTokenCookie(loginDto.getEmail());
            ResponseCookie refreshCookie = jwtTokenProvider.createRefreshTokenCookie(loginDto.getEmail());

            log.info(">>>> [Controller] 유저 {} 로그인 성공, 쿠키 발급", loginDto.getEmail());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto); // 닉네임, 팀 정보 등은 여전히 프론트에서 필요함
        } else {
            return ResponseEntity.status(401).body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
        }
    }

    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Principal principal) {
        if (principal != null) {
            memberService.logout(principal.getName());
        }

        // [개선] 빈 쿠키를 생성하여 브라우저의 쿠키 삭제 유도
        ResponseCookie emptyAccess = jwtTokenProvider.createEmptyCookie("accessToken");
        ResponseCookie emptyRefresh = jwtTokenProvider.createEmptyCookie("refreshToken");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyAccess.toString())
                .header(HttpHeaders.SET_COOKIE, emptyRefresh.toString())
                .body("로그아웃이 완료되었습니다.");
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<String> withdraw(Principal principal) {
        log.info(">>>> [Controller] 유저 {}의 회원탈퇴 요청", principal.getName());
        memberService.withdraw(principal.getName());

        // 탈퇴 시에도 쿠키 삭제
        ResponseCookie emptyAccess = jwtTokenProvider.createEmptyCookie("accessToken");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyAccess.toString())
                .body("회원탈퇴가 처리되었습니다. 이용해주셔서 감사합니다.");
    }

    // 관리자 용
    @GetMapping("/refresh-forbidden-words")
    public ResponseEntity<String> refreshWords() {
        memberService.refreshForbiddenWords();
        return ResponseEntity.ok("금칙어 캐시가 성공적으로 갱신되었습니다!");
    }
}