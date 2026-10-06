package com.dev.dugout.domain.postseason.service;

import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.repository.HitterRepository;
import com.dev.dugout.domain.player.repository.PitcherRepository;
import com.dev.dugout.domain.postseason.dto.PostseasonHotPlayersResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonHotPlayersResponseDto.HotBatterDto;
import com.dev.dugout.domain.postseason.dto.PostseasonHotPlayersResponseDto.HotPitcherDto;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 가을야구 5강 팀 선수들의 최근 폼(뜨거운 타자/투수) 집계.
 * 일별 성적 테이블은 시즌 누적 스냅샷이므로 (최신 누적 - N일 전 누적)으로 최근 N일 성적을 구한다.
 */
@Service
@RequiredArgsConstructor
public class PostseasonHotPlayerService {

    private static final int TOP5 = 5;
    private static final int HOT_BATTER_COUNT = 5;
    private static final int HOT_STARTER_COUNT = 3;
    private static final int HOT_RELIEVER_COUNT = 2;

    // 표본이 너무 작은 선수를 걸러내기 위한 기간 대비 최소 기준 (14일 기준: 28타석 / 선발 9.2이닝 / 불펜 4.1이닝)
    private static final double MIN_PA_PER_DAY = 2.0;
    private static final double MIN_STARTER_IP_PER_DAY = 0.7;
    private static final double MIN_RELIEVER_IP_PER_DAY = 0.3;

    private final DailyTeamRankingRepository dailyTeamRankingRepository;
    private final HitterRepository hitterRepository;
    private final PitcherRepository pitcherRepository;

    @Cacheable(value = "postseasonHotPlayers", key = "#days")
    public PostseasonHotPlayersResponseDto getHotPlayers(int days) {
        Set<Long> top5TeamIds = getTop5TeamIds();

        LocalDate endDate = hitterRepository.findMaxBaseDate();
        LocalDate baselineDate = endDate == null ? null : hitterRepository.findMaxBaseDateOnOrBefore(endDate.minusDays(days));

        return PostseasonHotPlayersResponseDto.builder()
                .days(days)
                .startDate(baselineDate != null ? baselineDate.plusDays(1) : null)
                .endDate(endDate)
                .hotBatters(getHotBatters(top5TeamIds, days))
                .hotPitchers(getHotPitchers(top5TeamIds, days))
                .build();
    }

    private Set<Long> getTop5TeamIds() {
        return dailyTeamRankingRepository.findMaxBaseDate()
                .map(latestDate -> dailyTeamRankingRepository.findAllByBaseDateOrderByRankAsc(latestDate).stream()
                        .filter(ranking -> ranking.getRank() != null && ranking.getRank() <= TOP5)
                        .map(ranking -> ranking.getTeam().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private List<HotBatterDto> getHotBatters(Set<Long> teamIds, int days) {
        LocalDate latestDate = hitterRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();
        LocalDate baselineDate = hitterRepository.findMaxBaseDateOnOrBefore(latestDate.minusDays(days));

        // 기준일 스냅샷이 없는 선수(기간 중 첫 출장)는 누적 전체가 곧 기간 성적이다
        Map<Long, DailyPlayerHitter> baselineByPlayer = baselineDate == null ? Map.of()
                : hitterRepository.findByBaseDateWithTeamAndPlayer(baselineDate).stream()
                .collect(Collectors.toMap(h -> h.getPlayer().getPlayerId(), Function.identity(), (a, b) -> a));

        return hitterRepository.findByBaseDateWithTeamAndPlayer(latestDate).stream()
                .filter(h -> h.getTeam() != null && teamIds.contains(h.getTeam().getId()))
                .map(h -> toHotBatter(h, baselineByPlayer.get(h.getPlayer().getPlayerId()), days))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HotBatterDto::getOps).reversed())
                .limit(HOT_BATTER_COUNT)
                .collect(Collectors.toList());
    }

    private HotBatterDto toHotBatter(DailyPlayerHitter latest, DailyPlayerHitter baseline, int days) {
        int pa = diff(latest, baseline, DailyPlayerHitter::getPa);
        int ab = diff(latest, baseline, DailyPlayerHitter::getAb);
        if (pa < days * MIN_PA_PER_DAY || ab <= 0) return null;

        int hits = diff(latest, baseline, DailyPlayerHitter::getH);
        int totalBases = diff(latest, baseline, DailyPlayerHitter::getTb);
        int walks = diff(latest, baseline, DailyPlayerHitter::getBb) + diff(latest, baseline, DailyPlayerHitter::getHbp);
        int obpDenominator = ab + walks + diff(latest, baseline, DailyPlayerHitter::getSf);

        double obp = obpDenominator > 0 ? (double) (hits + walks) / obpDenominator : 0;
        BigDecimal ops = scaled(obp + (double) totalBases / ab, 3);

        return HotBatterDto.builder()
                .playerName(latest.getPlayerName())
                .teamId(latest.getTeam().getId())
                .teamName(latest.getTeam().getName())
                .pa(pa)
                .avg(scaled((double) hits / ab, 3))
                .hr(diff(latest, baseline, DailyPlayerHitter::getHr))
                .rbi(diff(latest, baseline, DailyPlayerHitter::getRbi))
                .ops(ops)
                .seasonAvg(latest.getAvg())
                .seasonOps(latest.getOps())
                .opsDiff(latest.getOps() != null ? ops.subtract(latest.getOps()) : null)
                .build();
    }

    // 선발 3명 + 불펜 2명. 한 줄 세우기를 하면 짧게 던진 무실점 불펜이 상위를 독식하므로 보직별로 따로 뽑는다.
    private List<HotPitcherDto> getHotPitchers(Set<Long> teamIds, int days) {
        LocalDate latestDate = pitcherRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();
        LocalDate baselineDate = pitcherRepository.findMaxBaseDateOnOrBefore(latestDate.minusDays(days));

        Map<Long, DailyPlayerPitcher> baselineByPlayer = baselineDate == null ? Map.of()
                : pitcherRepository.findByBaseDateWithTeamAndPlayer(baselineDate).stream()
                .collect(Collectors.toMap(p -> p.getPlayer().getPlayerId(), Function.identity(), (a, b) -> a));

        List<HotPitcherDto> candidates = pitcherRepository.findByBaseDateWithTeamAndPlayer(latestDate).stream()
                .filter(p -> p.getTeam() != null && teamIds.contains(p.getTeam().getId()))
                .map(p -> toHotPitcher(p, baselineByPlayer.get(p.getPlayer().getPlayerId()), days))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HotPitcherDto::getEra)
                        .thenComparing(HotPitcherDto::getIp, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return Stream.concat(
                candidates.stream().filter(p -> "STARTER".equals(p.getRole())).limit(HOT_STARTER_COUNT),
                candidates.stream().filter(p -> "RELIEVER".equals(p.getRole())).limit(HOT_RELIEVER_COUNT)
        ).collect(Collectors.toList());
    }

    private HotPitcherDto toHotPitcher(DailyPlayerPitcher latest, DailyPlayerPitcher baseline, int days) {
        int games = diff(latest, baseline, DailyPlayerPitcher::getG);
        int outs = toOuts(latest.getIp()) - (baseline != null ? toOuts(baseline.getIp()) : 0);
        if (games <= 0 || outs <= 0) return null;

        // 기간 내 등판의 절반 이상이 선발이면 선발 투수로 본다
        int starts = diff(latest, baseline, DailyPlayerPitcher::getGs);
        boolean starter = starts > 0 && starts * 2 >= games;
        double minIp = days * (starter ? MIN_STARTER_IP_PER_DAY : MIN_RELIEVER_IP_PER_DAY);
        if (outs < minIp * 3) return null;

        int earnedRuns = diff(latest, baseline, DailyPlayerPitcher::getEr);
        int baseRunners = diff(latest, baseline, DailyPlayerPitcher::getH) + diff(latest, baseline, DailyPlayerPitcher::getBb);
        BigDecimal era = scaled(earnedRuns * 27.0 / outs, 2);

        return HotPitcherDto.builder()
                .role(starter ? "STARTER" : "RELIEVER")
                .playerName(latest.getPlayerName())
                .teamId(latest.getTeam().getId())
                .teamName(latest.getTeam().getName())
                .games(games)
                .ip(scaled(outs / 3.0, 2))
                .era(era)
                .whip(scaled(baseRunners * 3.0 / outs, 2))
                .so(diff(latest, baseline, DailyPlayerPitcher::getSo))
                .w(diff(latest, baseline, DailyPlayerPitcher::getW))
                .sv(diff(latest, baseline, DailyPlayerPitcher::getSv))
                .hld(diff(latest, baseline, DailyPlayerPitcher::getHld))
                .seasonEra(latest.getEra())
                .eraDiff(latest.getEra() != null ? era.subtract(latest.getEra()) : null)
                .build();
    }

    // 누적 스탯의 기간 증가분 (기준 스냅샷이 없거나 값이 null이면 0으로 본다)
    private <T> int diff(T latest, T baseline, Function<T, Integer> stat) {
        Integer latestValue = stat.apply(latest);
        Integer baselineValue = baseline != null ? stat.apply(baseline) : null;
        return (latestValue != null ? latestValue : 0) - (baselineValue != null ? baselineValue : 0);
    }

    // DB의 이닝은 ⅓ 단위 소수(5.33, 141.66)로 저장되므로 아웃카운트로 바꿔서 계산한다
    private int toOuts(BigDecimal ip) {
        return ip == null ? 0 : (int) Math.round(ip.doubleValue() * 3);
    }

    private BigDecimal scaled(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
