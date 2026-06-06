package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원가입 요청 DTO")
@Getter
@NoArgsConstructor
public class SignupRequestDto {

    @Schema(description = "닉네임 (금칙어 필터 적용)", example = "야구팬123")
    private String nickname;

    @Schema(description = "이메일 (로그인 ID로 사용)", example = "fan@dugout.cloud")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 영문과 숫자를 포함하여 8자 이상이어야 합니다."
    )
    @Schema(description = "비밀번호 (영문 + 숫자 포함 8자 이상)", example = "Pass1234")
    private String password;

    @Schema(description = "응원 구단명 (한글 풀네임)", example = "KIA 타이거즈")
    private String favoriteTeamName;
}
