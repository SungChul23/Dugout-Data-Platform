package com.dev.dugout.domain.postseason.service;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.repository.HitterRepository;
import com.dev.dugout.domain.player.repository.PitcherRepository;
import com.dev.dugout.domain.postseason.dto.PostseasonBracketResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTopPlayersResponseDto;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostseasonService {

    private static final int TOP5 = 5;
    private static final double MIN_PA_FOR_TOP_BATTER = 30;
    private static final double MIN_IP_FOR_TOP_PITCHER = 10;
    private static final int TOP_PLAYER_COUNT = 3;

    private final DailyTeamRankingRepository dailyTeamRankingRepository;
    private final TeamRepository teamRepository;
    private final HitterRepository hitterRepository;
    private final PitcherRepository pitcherRepository;

    @Cacheable(value = "postseasonBracket")
    public PostseasonBracketResponseDto getBracket() {
        LocalDate latestDate = dailyTeamRankingRepository.findMaxBaseDate().orElse(null);
        if (latestDate == null) {
            return PostseasonBracketResponseDto.builder().top5Teams(List.of()).matchups(List.of()).build();
        }

        // 순위 그래프용 findAllRankingsWithTeam()은 전체 시즌 히스토리를 반환하므로 쓰지 않고,
        // 최신 날짜의 순위만 직접 조회한다 (findAllByBaseDateOrderByRankAsc는 이미 rank 오름차순 정렬됨)
        List<TeamRankResponseDto> top5Teams = dailyTeamRankingRepository.findAllByBaseDateOrderByRankAsc(latestDate).stream()
                .filter(ranking -> ranking.getRank() != null && ranking.getRank() <= TOP5)
                .map(this::toTeamRankResponseDto)
                .collect(Collectors.toList());

        return PostseasonBracketResponseDto.builder()
                .top5Teams(top5Teams)
                .matchups(buildMatchups(top5Teams))
                .build();
    }

    private TeamRankResponseDto toTeamRankResponseDto(DailyTeamRanking entity) {
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
                .totalGames(144)
                .winRate(entity.getWinRate())
                .wins(entity.getWins())
                .gamesPlayed(entity.getGamesPlayed())
                .last10games(entity.getLast10Games())
                .build();
    }

    // KBO 포스트시즌 규정: 4위vs5위(WC) 승자가 3위와 준PO, 그 승자가 2위와 PO, 그 승자가 1위와 한국시리즈
    private List<PostseasonBracketResponseDto.MatchupDto> buildMatchups(List<TeamRankResponseDto> top5Teams) {
        Map<Integer, TeamRankResponseDto> byRank = top5Teams.stream()
                .collect(Collectors.toMap(TeamRankResponseDto::getTeamRank, team -> team));

        List<PostseasonBracketResponseDto.MatchupDto> matchups = new ArrayList<>();
        matchups.add(matchup("WILD_CARD", byRank.get(4), byRank.get(5)));
        matchups.add(matchup("SEMI_PLAYOFF", byRank.get(3), null));
        matchups.add(matchup("PLAYOFF", byRank.get(2), null));
        matchups.add(matchup("KOREAN_SERIES", byRank.get(1), null));
        return matchups;
    }

    private PostseasonBracketResponseDto.MatchupDto matchup(String round, TeamRankResponseDto higherSeed, TeamRankResponseDto lowerSeed) {
        return PostseasonBracketResponseDto.MatchupDto.builder()
                .round(round)
                .higherSeedTeamId(higherSeed != null ? higherSeed.getTeamId() : null)
                .higherSeedTeamName(higherSeed != null ? higherSeed.getTeamName() : "TBD")
                .lowerSeedTeamId(lowerSeed != null ? lowerSeed.getTeamId() : null)
                .lowerSeedTeamName(lowerSeed != null ? lowerSeed.getTeamName() : "TBD")
                .build();
    }

    @Cacheable(value = "postseasonTopPlayers", key = "#teamId")
    public Optional<PostseasonTopPlayersResponseDto> getTeamTopPlayers(Long teamId) {
        return teamRepository.findById(teamId)
                .map(team -> PostseasonTopPlayersResponseDto.builder()
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .topBatters(getTopBatters(teamId))
                        .topPitchers(getTopPitchers(teamId))
                        .build());
    }

    private List<PostseasonTopPlayersResponseDto.BatterStatDto> getTopBatters(Long teamId) {
        LocalDate latestDate = hitterRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        return hitterRepository.findByBaseDateWithTeam(latestDate).stream()
                .filter(h -> h.getTeam() != null && h.getTeam().getId() == teamId)
                .filter(h -> h.getPa() != null && h.getPa() >= MIN_PA_FOR_TOP_BATTER)
                .filter(h -> h.getOps() != null)
                .sorted(Comparator.comparing(DailyPlayerHitter::getOps).reversed())
                .limit(TOP_PLAYER_COUNT)
                .map(h -> PostseasonTopPlayersResponseDto.BatterStatDto.builder()
                        .playerName(h.getPlayerName())
                        .avg(h.getAvg())
                        .hr(h.getHr())
                        .ops(h.getOps())
                        .build())
                .collect(Collectors.toList());
    }

    private List<PostseasonTopPlayersResponseDto.PitcherStatDto> getTopPitchers(Long teamId) {
        LocalDate latestDate = pitcherRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        return pitcherRepository.findByBaseDateWithTeam(latestDate).stream()
                .filter(p -> p.getTeam() != null && p.getTeam().getId() == teamId)
                .filter(p -> p.getIp() != null && p.getIp().doubleValue() >= MIN_IP_FOR_TOP_PITCHER)
                .filter(p -> p.getEra() != null)
                .sorted(Comparator.comparing(DailyPlayerPitcher::getEra))
                .limit(TOP_PLAYER_COUNT)
                .map(p -> PostseasonTopPlayersResponseDto.PitcherStatDto.builder()
                        .playerName(p.getPlayerName())
                        .era(p.getEra())
                        .w(p.getW())
                        .so(p.getSo())
                        .build())
                .collect(Collectors.toList());
    }
}
