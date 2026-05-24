package com.dev.dugout.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

// 서버 → 프론트 로 나가는 데이터 구조
@Getter
@Builder
public class ChatResponseDto {
    private String answer; // 응답
    private String conversationId; // 사용자 uuid
}
