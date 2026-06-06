package com.dev.dugout.domain.chat.controller;

import com.dev.dugout.domain.chat.dto.ChatRequestDto;
import com.dev.dugout.domain.chat.dto.ChatResponseDto;
import com.dev.dugout.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "더그아웃 AI 챗봇 API — 자연어 질문 → SQL 생성 → RDS 조회 → 자연어 답변")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "질문 내용이 비어 있음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "KBO 야구 AI 챗봇 질문",
            description = """
                    자연어 질문을 받아 KBO 데이터를 조회하고 AI가 자연어로 답변합니다.

                    **처리 흐름**: 입력 검증 → 야구 도메인 확인 → 스키마 라우팅 → SQL 생성 → RDS 실행 → 자연어 변환

                    - `conversationId`는 대화 세션 식별자입니다. 동일한 ID로 요청하면 이전 대화 맥락이 유지됩니다 (30분 TTL).
                    - SQL 직접 입력 및 민감 정보 요청은 차단됩니다.
                    """
    )
    @RequestBody(
            description = "챗봇 질문 요청",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "타율 상위 조회 예시",
                            value = """
                                    {
                                      "message": "현재 타율 상위 3명 보여줘",
                                      "conversationId": "user-uuid-1234"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "AI 답변 반환 성공")
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@org.springframework.web.bind.annotation.RequestBody ChatRequestDto request) {

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
