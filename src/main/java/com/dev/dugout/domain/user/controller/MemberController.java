package com.dev.dugout.domain.user.controller;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.dto.NicknameCheckResponseDto;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import com.dev.dugout.domain.user.service.MemberService;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "User", description = "회원 인증 및 개인화 대시보드 API")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "회원가입",
            description = """
                    신규 회원을 등록하고 자동 로그인합니다.
                    성공 시 `accessToken`·`refreshToken` 쿠키를 Set-Cookie 헤더로 발급합니다.

                    **비밀번호 조건**: 영문 + 숫자 포함 8자 이상
                    """
    )
    @RequestBody(
            description = "회원가입 요청 정보",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "회원가입 예시",
                            value = """
                                    {
                                      "nickname": "야구팬123",
                                      "email": "fan@dugout.cloud",
                                      "password": "Pass1234",
                                      "favoriteTeamName": "KIA 타이거즈"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "회원가입 성공, 쿠키 발급")
    @ApiResponse(responseCode = "400", description = "유효성 검증 실패 (비밀번호 형식 오류 등)")
    @PostMapping("/signup")
    public ResponseEntity<LoginResponseDto> signup(@Valid @org.springframework.web.bind.annotation.RequestBody SignupRequestDto requestDto) {
        LoginResponseDto responseDto = memberService.signup(requestDto);

        ResponseCookie accessCookie = jwtTokenProvider.createAccessTokenCookie(requestDto.getEmail());
        ResponseCookie refreshCookie = jwtTokenProvider.createRefreshTokenCookie(requestDto.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseDto);
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "회원가입 전 닉네임 사용 가능 여부를 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "닉네임 확인 결과 반환")
    @GetMapping("/check-id")
    public ResponseEntity<NicknameCheckResponseDto> checkNickname(
            @Parameter(description = "확인할 닉네임", example = "야구팬123", required = true)
            @RequestParam String nickname) {
        return ResponseEntity.ok(memberService.checkNicknameAvailability(nickname));
    }

    @Operation(
            summary = "로그인",
            description = """
                    이메일/비밀번호로 로그인합니다.
                    성공 시 `accessToken`·`refreshToken` 쿠키를 발급하며, 응답 바디에 토큰은 포함되지 않습니다.
                    """
    )
    @RequestBody(
            description = "로그인 요청 정보",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "로그인 예시",
                            value = """
                                    {
                                      "email": "fan@dugout.cloud",
                                      "password": "Pass1234"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공, 쿠키 발급")
    @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    @PostMapping("/login")
    public ResponseEntity<?> login(@org.springframework.web.bind.annotation.RequestBody LoginRequestDto loginDto) {
        LoginResponseDto responseDto = memberService.getLoginUserInfo(loginDto);

        if (responseDto != null) {
            ResponseCookie accessCookie = jwtTokenProvider.createAccessTokenCookie(loginDto.getEmail());
            ResponseCookie refreshCookie = jwtTokenProvider.createRefreshTokenCookie(loginDto.getEmail());

            responseDto.setAccessToken(null);
            responseDto.setRefreshToken(null);

            log.info(">>>> [Controller] 유저 {} 로그인 성공, 쿠키 발급", loginDto.getEmail());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseDto);
        }
        return ResponseEntity.status(401).body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 세션을 종료하고 쿠키를 만료시킵니다. 로그인 상태에서만 호출 가능합니다."
    )
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Parameter(hidden = true) Principal principal) {
        if (principal != null) {
            memberService.logout(principal.getName());
        }

        ResponseCookie emptyAccess = jwtTokenProvider.createEmptyCookie("accessToken");
        ResponseCookie emptyRefresh = jwtTokenProvider.createEmptyCookie("refreshToken");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyAccess.toString())
                .header(HttpHeaders.SET_COOKIE, emptyRefresh.toString())
                .body("로그아웃이 완료되었습니다.");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자 계정을 삭제하고 쿠키를 만료시킵니다."
    )
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @DeleteMapping("/me")
    public ResponseEntity<String> withdraw(@Parameter(hidden = true) Principal principal) {
        log.info(">>>> [Controller] 유저 {}의 회원탈퇴 요청", principal.getName());
        memberService.withdraw(principal.getName());

        ResponseCookie emptyAccess = jwtTokenProvider.createEmptyCookie("accessToken");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyAccess.toString())
                .body("회원탈퇴가 처리되었습니다. 이용해주셔서 감사합니다.");
    }

    @Operation(
            summary = "내 정보 조회",
            description = "쿠키 기반으로 현재 로그인한 사용자의 닉네임·응원팀 정보를 반환합니다. 페이지 새로고침 시 로그인 상태 유지에 사용됩니다."
    )
    @ApiResponse(responseCode = "200", description = "사용자 정보 반환 성공")
    @ApiResponse(responseCode = "401", description = "로그인되지 않은 사용자")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@Parameter(hidden = true) Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인되지 않은 사용자입니다.");
        }

        LoginResponseDto userInfo = memberService.getMemberInfo(principal.getName());
        return ResponseEntity.ok(userInfo);
    }

    @Operation(
            summary = "금칙어 캐시 갱신",
            description = "관리자 전용: 서버에 캐시된 금칙어 목록을 DB에서 다시 로드합니다."
    )
    @ApiResponse(responseCode = "200", description = "금칙어 캐시 갱신 성공")
    @GetMapping("/refresh-forbidden-words")
    public ResponseEntity<String> refreshWords() {
        memberService.refreshForbiddenWords();
        return ResponseEntity.ok("금칙어 캐시가 성공적으로 갱신되었습니다!");
    }
}
