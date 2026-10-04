package com.dev.dugout.domain.postseason.service;

import com.dev.dugout.domain.postseason.dto.PostseasonSummaryGenerateResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTeamOverviewResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTopPlayersResponseDto;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.entity.TeamSeasonSummary;
import com.dev.dugout.domain.team.repository.DailyTeamStatsRepository;
import com.dev.dugout.domain.team.repository.TeamSeasonSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// 5강 팀의 시즌 종합 요약을 Bedrock으로 생성해 team_season_summary에 저장한다 (조회 시점에는 Bedrock을 호출하지 않음)
@Service
@RequiredArgsConstructor
@Slf4j
public class PostseasonSummaryService {

    private final PostseasonService postseasonService;
    private final PostseasonSummaryBedrockService postseasonSummaryBedrockService;
    private final DailyTeamStatsRepository dailyTeamStatsRepository;
    private final TeamSeasonSummaryRepository teamSeasonSummaryRepository;

    @CacheEvict(value = "postseasonTeamOverview", allEntries = true)
    public List<PostseasonSummaryGenerateResponseDto> generateSummaries() {
        List<DailyTeamStats> leagueStats = dailyTeamStatsRepository.findMaxBaseDate()
                .map(dailyTeamStatsRepository::findAllByBaseDate)
                .orElse(List.of());

        List<PostseasonSummaryGenerateResponseDto> results = new ArrayList<>();
        for (TeamRankResponseDto team : postseasonService.getBracket().getTop5Teams()) {
            Long teamId = team.getTeamId();
            PostseasonTeamOverviewResponseDto overview = postseasonService.getTeamOverview(teamId).orElse(null);
            if (overview == null || overview.getSeason() == null) continue;

            String teamData = buildTeamData(overview,
                    postseasonService.getTeamTopPlayers(teamId).orElse(null), leagueStats);
            String summary = postseasonSummaryBedrockService.generateSummary(teamData);

            if (summary != null) {
                teamSeasonSummaryRepository.save(TeamSeasonSummary.builder()
                        .teamId(teamId)
                        .season(overview.getSeason().longValue())
                        .summary(summary)
                        .build());
                log.info(">>>> [Postseason] 시즌 요약 저장 완료: {} ({})", overview.getTeamName(), overview.getSeason());
            } else {
                log.warn(">>>> [Postseason] 시즌 요약 생성 실패: {}", overview.getTeamName());
            }

            results.add(PostseasonSummaryGenerateResponseDto.builder()
                    .teamId(teamId)
                    .teamName(overview.getTeamName())
                    .season(overview.getSeason())
                    .summary(summary)
                    .build());
        }
        return results;
    }

    private String buildTeamData(PostseasonTeamOverviewResponseDto overview,
                                 PostseasonTopPlayersResponseDto topPlayers,
                                 List<DailyTeamStats> leagueStats) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- 구단: %s / 시즌: %d\n", overview.getTeamName(), overview.getSeason()));
        sb.append(String.format("- 정규시즌: %d위, %d승 %d패 %d무, 승률 %s\n",
                overview.getTeamRank(), overview.getWins(), overview.getLosses(), overview.getDraws(),
                overview.getWinRate()));

        // 타율/홈런은 높을수록, ERA는 낮을수록 상위
        if (overview.getTeamAvg() != null) {
            sb.append(String.format("- 팀 타율 %s (리그 %d위)\n", overview.getTeamAvg(),
                    leagueRank(leagueStats, DailyTeamStats::getAvgh1, overview.getTeamAvg(), true)));
        }
        if (overview.getTeamEra() != null) {
            sb.append(String.format("- 팀 ERA %s (리그 %d위)\n", overview.getTeamEra(),
                    leagueRank(leagueStats, DailyTeamStats::getErap1, overview.getTeamEra(), false)));
        }
        if (overview.getTeamHr() != null) {
            sb.append(String.format("- 팀 홈런 %d개 (리그 %d위)\n", overview.getTeamHr(),
                    leagueRank(leagueStats, s -> s.getHrh1() != null ? BigDecimal.valueOf(s.getHrh1()) : null,
                            BigDecimal.valueOf(overview.getTeamHr()), true)));
        }

        if (topPlayers != null) {
            sb.append("- 타자 TOP3(OPS 순): ").append(topPlayers.getTopBatters().stream()
                    .map(b -> String.format("%s(타율 %s, %d홈런, OPS %s)", b.getPlayerName(), b.getAvg(), b.getHr(), b.getOps()))
                    .collect(Collectors.joining(", "))).append("\n");
            sb.append("- 투수 TOP3(ERA 순): ").append(topPlayers.getTopPitchers().stream()
                    .map(p -> String.format("%s(ERA %s, %d승, %d탈삼진)", p.getPlayerName(), p.getEra(), p.getW(), p.getSo()))
                    .collect(Collectors.joining(", "))).append("\n");
        }
        return sb.toString();
    }

    // 해당 값보다 좋은 기록을 가진 팀 수 + 1
    private long leagueRank(List<DailyTeamStats> leagueStats, Function<DailyTeamStats, BigDecimal> getter,
                            BigDecimal value, boolean higherIsBetter) {
        return 1 + leagueStats.stream()
                .map(getter)
                .filter(other -> other != null
                        && (higherIsBetter ? other.compareTo(value) > 0 : other.compareTo(value) < 0))
                .count();
    }
}
