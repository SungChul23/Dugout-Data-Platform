package com.dev.dugout.domain.chat.controller;

import com.dev.dugout.domain.chat.dto.ChatRequestDto;
import com.dev.dugout.domain.chat.dto.ChatResponseDto;
import com.dev.dugout.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {

        log.info("[ChatController] 챗봇 요청 | conversationId: {} | message: {}",
                request.getConversationId(), request.getMessage());

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ChatResponseDto.builder()
                            .answer("질문을 입력해주세요.")
                            .conversationId(request.getConversationId())
                            .build());
        }

        String answer = chatService.chat(
                request.getMessage().trim(),
                request.getConversationId()
        );

        return ResponseEntity.ok(ChatResponseDto.builder()
                .answer(answer)
                .conversationId(request.getConversationId())
                .build());
    }
}