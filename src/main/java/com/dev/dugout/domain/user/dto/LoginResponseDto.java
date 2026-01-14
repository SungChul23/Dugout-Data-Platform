package com.dev.dugout.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private String nickname;
    private String favoriteTeamName;
    private String teamSlogan;
}
