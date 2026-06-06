package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청 DTO")
@Getter
@NoArgsConstructor
public class LoginRequestDto {

    @Schema(description = "이메일 (로그인 ID)", example = "fan@dugout.cloud")
    private String email;

    @Schema(description = "비밀번호", example = "Pass1234")
    private String password;
}
