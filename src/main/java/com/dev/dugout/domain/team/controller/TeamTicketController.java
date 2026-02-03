package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.TeamTicketResponseDto;
import com.dev.dugout.domain.team.service.TeamTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TeamTicketController {

    private final TeamTicketService teamTicketService;

    //전체 구단 예매 정보 조회
    @GetMapping("/teams")
    public ResponseEntity<List<TeamTicketResponseDto>> getAllTeamTickets() {
        // 서비스에서 모든 구단 정보를 List 보따리에 담아 옴
        List<TeamTicketResponseDto> response = teamTicketService.getAllTeamBookingInfo();

        // 보따리 채로 프론트엔드에게 단 한 번의 응답으로
        return ResponseEntity.ok(response);
    }
}
