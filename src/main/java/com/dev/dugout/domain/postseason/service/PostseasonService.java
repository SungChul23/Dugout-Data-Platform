package com.dev.dugout.domain.postseason.service;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.repository.HitterRepository;
import com.dev.dugout.domain.player.repository.PitcherRepository;
import com.dev.dugout.domain.postseason.dto.PostseasonBracketResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTeamOverviewResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTopPlayersResponseDto;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.repository.DailyTeamStatsRepository;
import com.dev.dugout.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostseasonService {

    private static final int TOP5 = 5;
    private static final int REGULAR_SEASON_GAMES = 144;
    private static final double QUALIFIED_PA_PER_GAME = 3.1;       // 규정타석 = 팀 경기수 × 3.1
    private static final double MIN_RELIEVER_IP_PER_GAME = 1.0 / 3; // 불펜은 규정이닝을 채울 수 없어 팀 경기수 ÷ 3 이닝을 기준으로 한다
    private static final int STARTER_COUNT = 2;
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final DailyTeamRankingRepository dailyTeamRankingRepository;
    private final DailyTeamStatsRepository dailyTeamStatsRepository;
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
                        .topBatters(getTopBatters(teamId, getTeamGamesPlayed(teamId)))
                        .topPitchers(getTopPitchers(teamId, getTeamGamesPlayed(teamId)))
                        .build());
    }

    // 규정타석/규정이닝 계산 기준이 되는 팀 경기수 (순위 데이터가 없으면 정규시즌 전체 경기수로 본다)
    private int getTeamGamesPlayed(Long teamId) {
        return dailyTeamRankingRepository.findMaxBaseDate()
                .flatMap(latestDate -> dailyTeamRankingRepository.findByBaseDateAndTeamId(latestDate, teamId))
                .map(DailyTeamRanking::getGamesPlayed)
                .orElse(REGULAR_SEASON_GAMES);
    }

    // 규정타석을 채운 타자 중 타율/홈런/OPS 부문에서 한 명씩 뽑는다.
    // 한 선수가 여러 부문 1위면 뒤 부문은 아직 뽑히지 않은 선수 중 최고 기록자로 채운다.
    private List<PostseasonTopPlayersResponseDto.BatterStatDto> getTopBatters(Long teamId, int teamGames) {
        LocalDate latestDate = hitterRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        int qualifiedPa = (int) (teamGames * QUALIFIED_PA_PER_GAME);
        List<DailyPlayerHitter> qualified = hitterRepository.findByBaseDateWithTeam(latestDate).stream()
                .filter(h -> h.getTeam() != null && h.getTeam().getId() == teamId)
                .filter(h -> h.getPa() != null && h.getPa() >= qualifiedPa)
                .filter(h -> h.getAvg() != null && h.getHr() != null && h.getOps() != null)
                .collect(Collectors.toList());

        Map<String, Comparator<DailyPlayerHitter>> categories = new LinkedHashMap<>();
        categories.put("AVG", Comparator.comparing(DailyPlayerHitter::getAvg));
        categories.put("HR", Comparator.comparing(DailyPlayerHitter::getHr).thenComparing(DailyPlayerHitter::getOps));
        categories.put("OPS", Comparator.comparing(DailyPlayerHitter::getOps));

        List<DailyPlayerHitter> picked = new ArrayList<>();
        List<PostseasonTopPlayersResponseDto.BatterStatDto> topBatters = new ArrayList<>();
        categories.forEach((category, comparator) -> qualified.stream()
                .filter(h -> !picked.contains(h))
                .max(comparator)
                .ifPresent(h -> {
                    picked.add(h);
                    topBatters.add(PostseasonTopPlayersResponseDto.BatterStatDto.builder()
                            .category(category)
                            .playerName(h.getPlayerName())
                            .avg(h.getAvg())
                            .hr(h.getHr())
                            .ops(h.getOps())
                            .pa(h.getPa())
                            .build());
                }));
        return topBatters;
    }

    // 선발 2명(규정이닝 충족, ERA 낮은 순) + 불펜 1명(팀 경기수 ÷ 3 이닝 이상, ERA 낮은 순)
    private List<PostseasonTopPlayersResponseDto.PitcherStatDto> getTopPitchers(Long teamId, int teamGames) {
        LocalDate latestDate = pitcherRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        List<DailyPlayerPitcher> pitchers = pitcherRepository.findByBaseDateWithTeam(latestDate).stream()
                .filter(p -> p.getTeam() != null && p.getTeam().getId() == teamId)
                .filter(p -> p.getIp() != null && p.getEra() != null)
                .collect(Collectors.toList());
        Comparator<DailyPlayerPitcher> byEra = Comparator.comparing(DailyPlayerPitcher::getEra);

        List<DailyPlayerPitcher> starterPool = pitchers.stream().filter(this::isStarter).collect(Collectors.toList());
        List<DailyPlayerPitcher> starters = starterPool.stream()
                .filter(p -> p.getIp().doubleValue() >= teamGames)
                .sorted(byEra)
                .limit(STARTER_COUNT)
                .collect(Collectors.toCollection(ArrayList::new));
        // 규정이닝을 채운 선발이 2명이 안 되는 팀은 이닝을 많이 던진 선발 순으로 채운다
        if (starters.size() < STARTER_COUNT) {
            starterPool.stream()
                    .filter(p -> !starters.contains(p))
                    .sorted(Comparator.comparing(DailyPlayerPitcher::getIp).reversed())
                    .limit(STARTER_COUNT - starters.size())
                    .forEach(starters::add);
        }

        List<PostseasonTopPlayersResponseDto.PitcherStatDto> topPitchers = starters.stream()
                .map(p -> toPitcherStatDto(p, "STARTER"))
                .collect(Collectors.toCollection(ArrayList::new));
        pitchers.stream()
                .filter(p -> !isStarter(p))
                .filter(p -> p.getIp().doubleValue() >= teamGames * MIN_RELIEVER_IP_PER_GAME)
                .min(byEra)
                .ifPresent(p -> topPitchers.add(toPitcherStatDto(p, "RELIEVER")));
        return topPitchers;
    }

    // 등판의 절반 이상이 선발이면 선발 투수로 본다
    private boolean isStarter(DailyPlayerPitcher p) {
        return p.getGs() != null && p.getG() != null && p.getGs() > 0 && p.getGs() * 2 >= p.getG();
    }

    private PostseasonTopPlayersResponseDto.PitcherStatDto toPitcherStatDto(DailyPlayerPitcher p, String role) {
        return PostseasonTopPlayersResponseDto.PitcherStatDto.builder()
                .role(role)
                .playerName(p.getPlayerName())
                .era(p.getEra())
                .w(p.getW())
                .so(p.getSo())
                .ip(p.getIp())
                .sv(p.getSv())
                .hld(p.getHld())
                .build();
    }

    // 3월부터 일 단위로 다 내려주면 포인트가 너무 많아, 주(월~일)별 마지막 순위 1건만 남긴다.
    // 마지막 주는 최신 날짜가 남으므로 그래프 끝점은 항상 현재 순위와 일치한다.
    @Cacheable(value = "postseasonRankTrend", key = "#teamId")
    public Optional<List<TeamRankResponseDto>> getTeamRankTrend(Long teamId) {
        return teamRepository.findById(teamId).map(team -> {
            Map<LocalDate, DailyTeamRanking> lastOfWeek = new LinkedHashMap<>();
            for (DailyTeamRanking ranking : dailyTeamRankingRepository.findByTeamIdOrderByBaseDateAsc(teamId)) {
                lastOfWeek.put(ranking.getBaseDate().with(DayOfWeek.MONDAY), ranking);
            }
            return lastOfWeek.values().stream()
                    .map(this::toTeamRankResponseDto)
                    .collect(Collectors.toList());
        });
    }

    @Cacheable(value = "postseasonTeamOverview", key = "#teamId")
    public Optional<PostseasonTeamOverviewResponseDto> getTeamOverview(Long teamId) {
        return teamRepository.findById(teamId).map(this::toTeamOverview);
    }

    private PostseasonTeamOverviewResponseDto toTeamOverview(Team team) {
        Long teamId = team.getId();
        LocalDate latestDate = dailyTeamRankingRepository.findMaxBaseDate().orElse(null);
        DailyTeamRanking ranking = latestDate == null ? null
                : dailyTeamRankingRepository.findByBaseDateAndTeamId(latestDate, teamId).orElse(null);
        DailyTeamStats stats = dailyTeamStatsRepository.findFirstByTeamIdOrderByBaseDateDesc(teamId).orElse(null);

        // 시즌은 최신 순위 데이터의 연도 기준
        Integer season = latestDate != null ? latestDate.getYear() : null;

        return PostseasonTeamOverviewResponseDto.builder()
                .teamId(teamId)
                .teamName(team.getName())
                .slogan(team.getSlogan())
                .stadiumName(team.getStadiumName())
                .championshipCount(team.getChampionshipCount())
                .season(season)
                .teamRank(ranking != null ? ranking.getRank() : null)
                .winRate(ranking != null ? ranking.getWinRate() : null)
                .wins(ranking != null ? ranking.getWins() : null)
                .losses(ranking != null ? ranking.getLosses() : null)
                .draws(ranking != null ? ranking.getDraws() : null)
                .teamAvg(stats != null ? stats.getAvgh1() : null)
                .teamEra(stats != null ? stats.getErap1() : null)
                .teamHr(stats != null ? stats.getHrh1() : null)
                .homeRecord(ranking != null ? parseRecord(ranking.getHomeRecord()) : null)
                .awayRecord(ranking != null ? parseRecord(ranking.getAwayRecord()) : null)
                .build();
    }

    // KBO 순위표의 홈/원정 성적 문자열은 "승-무-패" 순서 (예: "42-0-30")
    private PostseasonTeamOverviewResponseDto.RecordDto parseRecord(String record) {
        if (record == null) return null;

        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(record);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        if (numbers.size() != 3) return null;

        return PostseasonTeamOverviewResponseDto.RecordDto.builder()
                .wins(numbers.get(0))
                .draws(numbers.get(1))
                .losses(numbers.get(2))
                .build();
    }
}
