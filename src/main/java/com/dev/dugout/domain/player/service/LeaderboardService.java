package com.dev.dugout.domain.player.service;


import com.dev.dugout.domain.player.dto.LeaderboardDto;
import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.repository.HitterRepository;
import com.dev.dugout.domain.player.repository.PitcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final HitterRepository hitterRepository;
    private final PitcherRepository pitcherRepository;

    // 타자 리더보드 캐싱
    @Cacheable(value = "hitterLeaderboard", key = "#type")
    public List<LeaderboardDto.Response> getHitterLeaderboard(String type) {
        LocalDate latestDate = hitterRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        List<DailyPlayerHitter> allHitters = hitterRepository.findByBaseDate(latestDate);
        List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> configs = "NORMAL".equalsIgnoreCase(type) ?
                getNormalHitterConfigs() : getAdvancedHitterConfigs();

        return configs.stream()
                .map(config -> buildLeaderboard(allHitters, config))
                .collect(Collectors.toList());
    }

    // 투수 리더보드 캐싱
    @Cacheable(value = "pitcherLeaderboard", key = "#type")
    public List<LeaderboardDto.Response> getPitcherLeaderboard(String type) {
        LocalDate latestDate = pitcherRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        List<DailyPlayerPitcher> allPitchers = pitcherRepository.findByBaseDate(latestDate);
        List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> configs = "NORMAL".equalsIgnoreCase(type) ?
                getNormalPitcherConfigs() : getAdvancedPitcherConfigs();

        return configs.stream()
                .map(config -> buildLeaderboard(allPitchers, config))
                .collect(Collectors.toList());
    }

    // 🏆 공통 정렬 및 포맷팅 로직
    private <T> LeaderboardDto.Response buildLeaderboard(List<T> data, LeaderboardDto.MetricConfig<T> config) {
        Comparator<T> comparator = Comparator.comparing(config.getExtractor());
        if (config.isDesc()) comparator = comparator.reversed(); // 내림차순(높은게 1등) 처리

        List<T> top5 = data.stream()
                .sorted(comparator)
                .limit(5)
                .collect(Collectors.toList());

        List<LeaderboardDto.RankItem> ranks = IntStream.range(0, top5.size())
                .mapToObj(i -> {
                    T entity = top5.get(i);
                    double rawValue = config.getExtractor().apply(entity);
                    String displayVal = config.getPrecision() == 0 ?
                            String.valueOf((int) rawValue) : String.format("%." + config.getPrecision() + "f", rawValue);

                    // TODO: 엔티티 구조에 맞게 getPlayerName, getTeamName 메소드 수정 필요
                    String pName = (entity instanceof DailyPlayerHitter) ? ((DailyPlayerHitter) entity).getPlayerName() : ((DailyPlayerPitcher) entity).getPlayerName();
                    String tName = "팀명"; // 팀명은 Team 엔티티 조인이나 로직에 맞게 주입

                    return LeaderboardDto.RankItem.builder()
                            .rank(i + 1)
                            .playerName(pName)
                            .teamName(tName)
                            .displayValue(displayVal)
                            .build();
                })
                .collect(Collectors.toList());

        return LeaderboardDto.Response.builder()
                .title(config.getTitle())
                .metricKey(config.getKey())
                .unit(config.getUnit())
                .ranks(ranks)
                .build();
    }


    //  지표 설정 매핑
    private List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> getNormalHitterConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("타율 (AVG)", "avg", "", true, 3, DailyPlayerHitter::getAvg),
                new LeaderboardDto.MetricConfig<>("홈런 (HR)", "hr", "개", true, 0, DailyPlayerHitter::getHr),
                new LeaderboardDto.MetricConfig<>("타점 (RBI)", "rbi", "점", true, 0, DailyPlayerHitter::getRbi),
                new LeaderboardDto.MetricConfig<>("안타 (H)", "h", "개", true, 0, DailyPlayerHitter::getH),
                new LeaderboardDto.MetricConfig<>("득점 (R)", "r", "점", true, 0, DailyPlayerHitter::getR),
                new LeaderboardDto.MetricConfig<>("OPS", "ops", "", true, 3, DailyPlayerHitter::getOps)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> getAdvancedHitterConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("출루율 (OBP)", "obp", "", true, 3, DailyPlayerHitter::getObp),
                new LeaderboardDto.MetricConfig<>("득점권 타율 (RISP)", "risp", "", true, 3, DailyPlayerHitter::getRisp),
                new LeaderboardDto.MetricConfig<>("장타율 (SLG)", "slg", "", true, 3, DailyPlayerHitter::getSlg),
                new LeaderboardDto.MetricConfig<>("득점 공헌도 (XR)", "xr", "", true, 2, DailyPlayerHitter::getXr),
                new LeaderboardDto.MetricConfig<>("장타 수 (XBH)", "xbh", "개", true, 0, DailyPlayerHitter::getXbh),
                new LeaderboardDto.MetricConfig<>("대타 타율 (PH BA)", "phBa", "", true, 3, DailyPlayerHitter::getPhBa)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> getNormalPitcherConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("평균자책점 (ERA)", "era", "", false, 2, DailyPlayerPitcher::getEra),
                new LeaderboardDto.MetricConfig<>("다승 (W)", "w", "승", true, 0, DailyPlayerPitcher::getW),
                new LeaderboardDto.MetricConfig<>("탈삼진 (SO)", "so", "개", true, 0, DailyPlayerPitcher::getSo),
                new LeaderboardDto.MetricConfig<>("세이브 (SV)", "sv", "세", true, 0, DailyPlayerPitcher::getSv),
                new LeaderboardDto.MetricConfig<>("홀드 (HLD)", "hld", "홀", true, 0, DailyPlayerPitcher::getHld),
                new LeaderboardDto.MetricConfig<>("WHIP", "whip", "", false, 2, DailyPlayerPitcher::getWhip)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> getAdvancedPitcherConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("퀄리티 스타트 (QS)", "qs", "회", true, 0, DailyPlayerPitcher::getQs),
                new LeaderboardDto.MetricConfig<>("피안타율 (AVG)", "avg", "", false, 3, DailyPlayerPitcher::getAvg),
                new LeaderboardDto.MetricConfig<>("승률 (WPCT)", "wpct", "", true, 3, DailyPlayerPitcher::getWpct),
                new LeaderboardDto.MetricConfig<>("이닝 (IP)", "ip", "이닝", true, 1, DailyPlayerPitcher::getIp),
                new LeaderboardDto.MetricConfig<>("GO/AO (땅볼 유도 능력)", "goAo", "", true, 2, DailyPlayerPitcher::getGoAo),
                new LeaderboardDto.MetricConfig<>("블론세이브 (BSV)", "bsv", "개", false, 0, DailyPlayerPitcher::getBsv)
        );
    }
}