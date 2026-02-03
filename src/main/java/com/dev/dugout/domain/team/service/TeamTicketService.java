package com.dev.dugout.domain.team.service;


import com.dev.dugout.domain.team.dto.TeamTicketResponseDto;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamTicketService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<TeamTicketResponseDto> getAllTeamBookingInfo() {
        //DB에서 모든 팀 정보를 가져옴
        List<Team> allTeams = teamRepository.findAll();

        //엔티티를 DTO로 변환하여 리스트로 반환
        return allTeams.stream()
                .map(team -> TeamTicketResponseDto.builder()
                        .id(team.getId())
                        .name(team.getName())
                        .city(team.getCity())
                        .stadiumName(team.getStadiumName())
                        .bookingUrl(team.getBookingUrl())
                        .build())
                .collect(Collectors.toList());
    }
}
