package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.TeamTicketResponseDto;
import com.dev.dugout.domain.team.service.TeamTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Team", description = "팀·경기·뉴스·티켓 관련 API")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TeamTicketController {

    private final TeamTicketService teamTicketService;

    @Operation(
            summary = "전체 구단 티켓 예매 정보 조회",
            description = "10개 KBO 구단의 홈구장 정보와 공식 티켓 예매 링크를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "티켓 예매 정보 반환 성공")
    @GetMapping("/teams")
    public ResponseEntity<List<TeamTicketResponseDto>> getAllTeamTickets() {
        List<TeamTicketResponseDto> response = teamTicketService.getAllTeamBookingInfo();
        return ResponseEntity.ok(response);
    }
}
