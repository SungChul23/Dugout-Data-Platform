package com.dev.dugout.domain.admin.controller;

import com.dev.dugout.domain.admin.dto.NoticeDto;
import com.dev.dugout.domain.admin.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "공지사항 관련 API")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(
            summary = "공지사항 전체 조회",
            description = "FEATURE / IMPROVEMENT / EVENT 유형을 포함한 전체 공지사항 목록을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "공지사항 목록 반환 성공")
    @GetMapping
    public ResponseEntity<List<NoticeDto>> getAll() {
        return ResponseEntity.ok(noticeService.findAllNotices());
    }
}
