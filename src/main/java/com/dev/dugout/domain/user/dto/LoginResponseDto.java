package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "로그인·회원가입 응답 DTO (토큰은 쿠키로 발급되며 바디에는 포함되지 않음)")
@Getter
@Setter
@AllArgsConstructor
public class LoginResponseDto {

    @Schema(description = "액세스 토큰 (응답 바디에서는 null, Set-Cookie 헤더로 전달)", example = "null")
    private String accessToken;

    @Schema(description = "리프레시 토큰 (응답 바디에서는 null, Set-Cookie 헤더로 전달)", example = "null")
    private String refreshToken;

    @Schema(description = "사용자 닉네임", example = "야구팬123")
    private String nickname;

    @Schema(description = "응원 구단명", example = "KIA 타이거즈")
    private String favoriteTeamName;

    @Schema(description = "응원 구단 슬로건", example = "압도하라! V13 ALWAYS KIA TIGERS")
    private String teamSlogan;
}
