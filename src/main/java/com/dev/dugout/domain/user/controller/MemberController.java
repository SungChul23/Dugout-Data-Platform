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
            ResponseCookie accessCookie = jwtTokenProvider.createAccessTokenCookie(loginDto.getEmail());
            ResponseCookie refreshCookie = jwtTokenProvider.createRefreshTokenCookie(loginDto.getEmail());

            // [중요] 토큰을 제외한 유저 정보만 담은 응답 객체를 새로 만들거나,
            // 기존 DTO에서 토큰 필드만 null로 밀어버립니다.
            responseDto.setAccessToken(null);
            responseDto.setRefreshToken(null);

            log.info(">>>> [Controller] 유저 {} 로그인 성공, 쿠키 발급", loginDto.getEmail());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto); // 이제 바디에는 닉네임, 팀 정보만 남음!
        }
        return ResponseEntity.status(401).body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
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
    // 내 정보 조회(새로고침 시 로그인 유지용)
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인되지 않은 사용자입니다.");
        }

        // principal.getName()에는 우리가 토큰에 넣었던 email(또는 loginId)이 들어있습니다.
        // 서비스를 통해 DB에서 해당 유저의 최신 정보를 가져옵니다.
        LoginResponseDto userInfo = memberService.getMemberInfo(principal.getName());

        return ResponseEntity.ok(userInfo);
    }


    // 관리자 용
    @GetMapping("/refresh-forbidden-words")
    public ResponseEntity<String> refreshWords() {
        memberService.refreshForbiddenWords();
        return ResponseEntity.ok("금칙어 캐시가 성공적으로 갱신되었습니다!");
    }
}