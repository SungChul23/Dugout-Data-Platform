package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.dto.NicknameCheckResponseDto;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import com.dev.dugout.domain.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<LoginResponseDto> signup(@RequestBody SignupRequestDto requestDto) {
        LoginResponseDto responseDto = memberService.signup(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // 닉네임 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<NicknameCheckResponseDto> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(memberService.checkNicknameAvailability(nickname));
    }


    //서비스에서 토큰 2개(Access/Refresh)가 포함된 DTO를 응답받음
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginDto) {
        LoginResponseDto responseDto = memberService.getLoginUserInfo(loginDto);

        if (responseDto != null) {
            // 성공 시 토큰 2개, 닉네임, 팀 정보를 모두 포함하여 반환
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.status(401).body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
        }
    }

    // 관리자 용
    @GetMapping("/refresh-forbidden-words")
    public ResponseEntity<String> refreshWords() {
        memberService.refreshForbiddenWords();
        return ResponseEntity.ok("금칙어 캐시가 성공적으로 갱신되었습니다!");
    }

    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Principal principal) {
        // principal.getName()을 하면 필터가 저장해둔 loginId가 바로 나옵니다.
        memberService.logout(principal.getName());
        return ResponseEntity.ok("로그아웃 성공");
    }
}