package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import com.dev.dugout.domain.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto requestDto) {
        memberService.signup(requestDto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 닉네임 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(memberService.isNicknameAvailable(nickname));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginDto) {
        // Service에서 로그인 검증 후 닉네임을 가져오거나 null 반환
        String nickname = memberService.getNicknameIfValid(loginDto);

        if (nickname != null) {
            return ResponseEntity.ok(new LoginResponseDto(nickname));
        } else {
            return ResponseEntity.status(401).body("로그인 실패");
        }
    }
}
