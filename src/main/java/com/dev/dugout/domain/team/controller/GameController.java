package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.GameResponseDto;
import com.dev.dugout.domain.team.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Team", description = "팀·경기·뉴스·티켓 관련 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 연도 또는 월 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(
            summary = "월별 경기 일정 및 결과 조회",
            description = """
                    특정 연도·월의 전 구단 경기 일정과 결과를 반환합니다.

                    - `status` 값: `SCHEDULED`(예정), `LIVE`(진행 중), `FINISHED`(종료), `CANCELED`(취소)
                    - 종료된 경기는 `homeScore`, `awayScore`가 포함됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "경기 목록 반환 성공")
    @GetMapping
    public ResponseEntity<List<GameResponseDto>> getSchedule(
            @Parameter(description = "조회 연도", example = "2026")
            @RequestParam(defaultValue = "2026") int year,
            @Parameter(description = "조회 월 (1~12)", example = "6", required = true)
            @RequestParam int month) {
        List<GameResponseDto> schedule = gameService.getMonthlySchedule(year, month);
        return ResponseEntity.ok(schedule);
    }
}
