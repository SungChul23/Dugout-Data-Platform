package com.dev.dugout.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;


@Getter
@NoArgsConstructor
public class SignupRequestDto {
    private String nickname;
    private String email;


    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 영문과 숫자를 포함하여 8자 이상이어야 합니다."
    )
    private String password;


    private String favoriteTeamName;
}