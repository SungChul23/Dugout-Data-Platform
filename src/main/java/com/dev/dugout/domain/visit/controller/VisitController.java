package com.dev.dugout.domain.visit.controller;

import com.dev.dugout.domain.visit.dto.VisitCountResponseDto;
import com.dev.dugout.domain.visit.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Visit", description = "메인 페이지 일일 방문자 수 API")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(
            summary = "방문 기록",
            description = "요청 IP 기준으로 오늘 방문을 기록하고 오늘 방문자 수를 반환합니다. 같은 IP는 하루에 한 번만 집계됩니다."
    )
    @ApiResponse(responseCode = "200", description = "방문 기록 성공")
    @PostMapping
    public ResponseEntity<VisitCountResponseDto> recordVisit(HttpServletRequest request) {
        return ResponseEntity.ok(visitService.recordVisit(resolveClientIp(request)));
    }

    // 프록시/로드밸런서를 거치면 실제 클라이언트 IP는 X-Forwarded-For의 첫 번째 값에 담긴다
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Operation(
            summary = "오늘 방문자 수 조회",
            description = "오늘(한국 시간 기준) 방문자 수를 기록 없이 조회합니다. 자정이 지나면 0부터 다시 집계됩니다."
    )
    @ApiResponse(responseCode = "200", description = "오늘 방문자 수 반환 성공")
    @GetMapping("/today")
    public ResponseEntity<VisitCountResponseDto> getTodayCount() {
        return ResponseEntity.ok(visitService.getTodayCount());
    }
}
