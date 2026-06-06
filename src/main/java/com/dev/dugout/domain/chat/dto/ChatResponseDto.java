package com.dev.dugout.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "챗봇 답변 응답 DTO")
@Getter
@Builder
public class ChatResponseDto {

    @Schema(description = "AI가 생성한 자연어 답변", example = "현재 타율 상위 3명은 김도영(.358), 이정후(.345), 나성범(.332)입니다.")
    private String answer;

    @Schema(description = "대화 세션 식별자 (요청과 동일하게 에코)", example = "user-uuid-1234-abcd")
    private String conversationId;
}
