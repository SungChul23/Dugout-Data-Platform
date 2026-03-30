package com.dev.dugout.domain.team.service;

import com.dev.dugout.domain.team.dto.TeamPerformanceResponseDto;
import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.repository.DailyTeamStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamPerformanceService {

    private final DailyTeamStatsRepository teamStatsRepository;

    @Transactional(readOnly = true)
    public List<TeamPerformanceResponseDto> getAllTeamPerformances() {
        // 1. DB에서 가장 최신 날짜 조회
        LocalDate latestDate = teamStatsRepository.findMaxBaseDate()
                .orElseThrow(() -> new RuntimeException("팀 스탯 데이터가 존재하지 않습니다."));

        // 2. 해당 날짜의 10개 구단 데이터 전체 조회
        return teamStatsRepository.findAllByBaseDate(latestDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TeamPerformanceResponseDto convertToDto(DailyTeamStats s) {
        return TeamPerformanceResponseDto.builder()
                .teamId(s.getTeam().getId())
                .teamName(s.getTeam().getName())
                .baseDate(s.getBaseDate()) // DTO에 기준일 매핑
                // 타자 12 (엔티티 필드명 규칙 반영: getAvgh2 등)
                .avg(s.getAvgh2()).hr(s.getHrh1()).runs(s.getRh1()).hits(s.getHh1())
                .rbi(s.getRbih1()).obp(s.getObph2()).ops(s.getOpsh2()).risp(s.getRisph2())
                .slg(s.getSlgh2()).phBa(s.getPhbah2()).multiHit(s.getMhh2()).totalBases(s.getTbh1())
                // 투수 12
                .era(s.getErap1()).wins(s.getWp1()).so(s.getSop1()).sv(s.getSvp1())
                .hld(s.getHldp1()).wpct(s.getWpctp1()).whip(s.getWhipp1()).qs(s.getQsp2())
                .oppAvg(s.getAvgp2()).bsv(s.getBsvp2()).np(s.getNpp2()).hrAllowed(s.getHrp1())
                // 수비/주루 12
                .sb(s.getSbr()).sbRate(s.getSbrater()).error(s.getEd()).fpct(s.getFpctd())
                .dp(s.getDpd()).csRate(s.getCsrated()).oob(s.getOobr()).sba(s.getSbar())
                .pkoR(s.getPkor()).pkoD(s.getPkod()).cs(s.getCsd()).sbAllowed(s.getSbd())
                .build();
    }
}