package com.dev.dugout.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 프론트 → 서버 로 오는 데이터 구조
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    private String message; // 자연어
    private String conversationId; // 사용자가 uuid (대화 기록용)
}
