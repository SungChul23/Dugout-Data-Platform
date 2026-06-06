package com.dev.dugout.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "챗봇 질문 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    @Schema(description = "사용자 자연어 질문", example = "현재 타율 상위 3명 보여줘")
    private String message;

    @Schema(description = "대화 세션 식별자 (UUID). 동일 ID로 연속 요청 시 대화 맥락이 유지됩니다 (30분 TTL).",
            example = "user-uuid-1234-abcd")
    private String conversationId;
}
