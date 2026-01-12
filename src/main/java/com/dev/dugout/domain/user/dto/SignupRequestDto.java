package com.dev.dugout.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {
    private String nickname;
    private String email;
    private String password;
    private String favoriteTeamName;
}