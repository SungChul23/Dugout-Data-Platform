package com.dev.dugout.domain.team.service;

import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamRankingService {
    private final DailyTeamRankingRepository rankingRepository;

    @Transactional(readOnly = true)
    public List<TeamRankResponseDto> getAllTeamRankings() {

        // 조건 없이 싹 다 가져와서 DTO로 변환 후 리턴
        return rankingRepository.findAllRankingsWithTeam().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TeamRankResponseDto convertToDto(DailyTeamRanking entity) {
        return TeamRankResponseDto.builder()
                .rankingDate(entity.getBaseDate())
                .teamId(entity.getTeam().getId())
                .teamName(entity.getTeam().getName())
                .awayRecord(entity.getAwayRecord())
                .draws(entity.getDraws())
                .gamesBehind(entity.getGamesBehind())
                .homeRecord(entity.getHomeRecord())
                .losses(entity.getLosses())
                .teamRank(entity.getRank())
                .recent10games(entity.getLast10Games())
                .streak(entity.getStreak())
                .totalGames(144) // 정규시즌 총 경기수 (정책에 따라 가공 가능)
                .winRate(entity.getWinRate())
                .wins(entity.getWins())
                .gamesPlayed(entity.getGamesPlayed())
                .last10games(entity.getLast10Games())
                .build();
    }
}
