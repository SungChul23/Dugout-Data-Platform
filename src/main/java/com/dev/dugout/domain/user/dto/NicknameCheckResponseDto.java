package com.dev.dugout.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "닉네임 중복 확인 응답 DTO")
@Getter
@AllArgsConstructor
public class NicknameCheckResponseDto {

    @Schema(description = "닉네임 사용 가능 여부 (true: 사용 가능)", example = "true")
    private boolean isAvailable;

    @Schema(description = "안내 메시지", example = "사용 가능한 닉네임입니다.")
    private String message;
}
