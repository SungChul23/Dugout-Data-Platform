package com.dev.dugout.domain.player.service;

import com.dev.dugout.domain.player.dto.LeaderboardDto;
import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.repository.HitterRepository;
import com.dev.dugout.domain.player.repository.PitcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final HitterRepository hitterRepository;
    private final PitcherRepository pitcherRepository;

    // ==========================================
    // 1. 타자 리더보드 서빙 (규정 타석 적용)
    // ==========================================
    @Cacheable(value = "hitterLeaderboard", key = "#type")
    public List<LeaderboardDto.Response> getHitterLeaderboard(String type) {
        LocalDate latestDate = hitterRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        // JOIN FETCH로 팀 정보까지 한 번에 로드 (N+1 방지)
        List<DailyPlayerHitter> allHitters = hitterRepository.findByBaseDateWithTeam(latestDate);

        // 규정 타석 계산: 최다 경기 출장수 * 3.1
        int maxGames = allHitters.stream().mapToInt(h -> h.getG() != null ? h.getG() : 0).max().orElse(1);
        double requiredPa = maxGames * 3.1;

        List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> configs = "NORMAL".equalsIgnoreCase(type) ?
                getNormalHitterConfigs() : getAdvancedHitterConfigs();

        return configs.stream()
                .map(config -> buildLeaderboard(allHitters, config, requiredPa, true))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 2. 투수 리더보드 서빙 (규정 이닝 적용)
    // ==========================================
    @Cacheable(value = "pitcherLeaderboard", key = "#type")
    public List<LeaderboardDto.Response> getPitcherLeaderboard(String type) {
        LocalDate latestDate = pitcherRepository.findMaxBaseDate();
        if (latestDate == null) return List.of();

        // JOIN FETCH로 팀 정보까지 한 번에 로드 (N+1 방지)
        List<DailyPlayerPitcher> allPitchers = pitcherRepository.findByBaseDateWithTeam(latestDate);

        // 규정 이닝 계산: 최다 경기 출장수 * 1.0
        int maxGames = allPitchers.stream().mapToInt(p -> p.getG() != null ? p.getG() : 0).max().orElse(1);
        double requiredIp = maxGames * 1.0;

        List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> configs = "NORMAL".equalsIgnoreCase(type) ?
                getNormalPitcherConfigs() : getAdvancedPitcherConfigs();

        return configs.stream()
                .map(config -> buildLeaderboard(allPitchers, config, requiredIp, false))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3.  공통 정렬, 필터, 공동 순위 로직
    // ==========================================
    private <T> LeaderboardDto.Response buildLeaderboard(List<T> data, LeaderboardDto.MetricConfig<T> config, double requiredAmount, boolean isHitter) {

        // 1. 비율 스탯 필터링 (규정 타석/이닝을 채운 선수만 통과)
        List<T> filteredData = data.stream()
                .filter(entity -> {
                    if (!config.isRateStat()) return true; // 누적 스탯은 무조건 통과

                    if (isHitter) {
                        Integer pa = ((DailyPlayerHitter) entity).getPa();
                        return pa != null && pa >= requiredAmount;
                    } else {
                        BigDecimal ip = ((DailyPlayerPitcher) entity).getIp();
                        return ip != null && ip.doubleValue() >= requiredAmount;
                    }
                })
                .collect(Collectors.toList());

        // 2. 정렬 로직 (오름차순/내림차순)
        Comparator<T> comparator = Comparator.comparing(config.getExtractor());
        if (config.isDesc()) comparator = comparator.reversed();

        List<T> top5 = filteredData.stream()
                .sorted(comparator)
                .limit(5)
                .collect(Collectors.toList());

        // 3. 공동 순위(Tie) 및 DTO 매핑 로직
        List<LeaderboardDto.RankItem> ranks = new ArrayList<>();
        int displayRank = 1;
        double prevValue = Double.MAX_VALUE;

        for (int i = 0; i < top5.size(); i++) {
            T entity = top5.get(i);
            double currentValue = config.getExtractor().apply(entity);

            // 값이 이전과 다르면 순위를 현재 인덱스 + 1 로 업데이트 (공동 순위 처리)
            if (i > 0 && currentValue != prevValue) {
                displayRank = i + 1;
            }

            // 소수점 포맷팅
            String displayVal = config.getPrecision() == 0 ?
                    String.valueOf((int) currentValue) : String.format("%." + config.getPrecision() + "f", currentValue);

            // 이름 및 팀명 안전하게 추출
            String pName = isHitter ? ((DailyPlayerHitter) entity).getPlayerName() : ((DailyPlayerPitcher) entity).getPlayerName();
            String tName = "팀명없음";

            if (isHitter && ((DailyPlayerHitter) entity).getTeam() != null) {
                tName = ((DailyPlayerHitter) entity).getTeam().getName();
            } else if (!isHitter && ((DailyPlayerPitcher) entity).getTeam() != null) {
                tName = ((DailyPlayerPitcher) entity).getTeam().getName();
            }

            ranks.add(LeaderboardDto.RankItem.builder()
                    .rank(displayRank)
                    .playerName(pName)
                    .teamName(tName)
                    .displayValue(displayVal)
                    .build());

            prevValue = currentValue;
        }

        return LeaderboardDto.Response.builder()
                .title(config.getTitle())
                .metricKey(config.getKey())
                .unit(config.getUnit())
                .ranks(ranks)
                .build();
    }

    // ==========================================
    // 4.  지표 매핑 (isRateStat 플래그 적용 완료)
    // ==========================================

    private List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> getNormalHitterConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("타율 (AVG)", "avg", "", true, 3, true, DailyPlayerHitter::getAvg),
                new LeaderboardDto.MetricConfig<>("홈런 (HR)", "hr", "개", true, 0, false, DailyPlayerHitter::getHr),
                new LeaderboardDto.MetricConfig<>("타점 (RBI)", "rbi", "점", true, 0, false, DailyPlayerHitter::getRbi),
                new LeaderboardDto.MetricConfig<>("안타 (H)", "h", "개", true, 0, false, DailyPlayerHitter::getH),
                new LeaderboardDto.MetricConfig<>("득점 (R)", "r", "점", true, 0, false, DailyPlayerHitter::getR),
                new LeaderboardDto.MetricConfig<>("OPS", "ops", "", true, 3, true, DailyPlayerHitter::getOps),
                new LeaderboardDto.MetricConfig<>("멀티히트 (MH)", "mh", "회", true, 0, false, DailyPlayerHitter::getMh),
                new LeaderboardDto.MetricConfig<>("루타 (TB)", "tb", "루타", true, 0, false, DailyPlayerHitter::getTb)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerHitter>> getAdvancedHitterConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("출루율 (OBP)", "obp", "", true, 3, true, DailyPlayerHitter::getObp),
                new LeaderboardDto.MetricConfig<>("득점권 타율 (RISP)", "risp", "", true, 3, true, DailyPlayerHitter::getRisp),
                new LeaderboardDto.MetricConfig<>("장타율 (SLG)", "slg", "", true, 3, true, DailyPlayerHitter::getSlg),
                new LeaderboardDto.MetricConfig<>("득점 공헌도 (XR)", "xr", "", true, 2, false, DailyPlayerHitter::getXr),
                new LeaderboardDto.MetricConfig<>("장타 수 (XBH)", "xbh", "개", true, 0, false, DailyPlayerHitter::getXbh),
                new LeaderboardDto.MetricConfig<>("순수장타율 (ISO)", "isop", "", true, 3, true, DailyPlayerHitter::getIsop),
                new LeaderboardDto.MetricConfig<>("결승타 (GW RBI)", "gwRbi", "개", true, 0, false, DailyPlayerHitter::getGwRbi),
                new LeaderboardDto.MetricConfig<>("삼진 (SO)", "so", "개", true, 0, true, DailyPlayerHitter::getSo)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> getNormalPitcherConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("평균자책점 (ERA)", "era", "", false, 2, true, DailyPlayerPitcher::getEra),
                new LeaderboardDto.MetricConfig<>("다승 (W)", "w", "승", true, 0, false, DailyPlayerPitcher::getW),
                new LeaderboardDto.MetricConfig<>("탈삼진 (SO)", "so", "개", true, 0, false, DailyPlayerPitcher::getSo),
                new LeaderboardDto.MetricConfig<>("세이브 (SV)", "sv", "세", true, 0, false, DailyPlayerPitcher::getSv),
                new LeaderboardDto.MetricConfig<>("홀드 (HLD)", "hld", "홀", true, 0, false, DailyPlayerPitcher::getHld),
                new LeaderboardDto.MetricConfig<>("이닝 (IP)", "ip", "이닝", true, 1, false, DailyPlayerPitcher::getIp),
                new LeaderboardDto.MetricConfig<>("볼넷 허용 (BB)", "bb", "개", true, 0, false, DailyPlayerPitcher::getBb),
                new LeaderboardDto.MetricConfig<>("실점 (R)", "r", "점", true, 0, false, DailyPlayerPitcher::getR)
        );
    }

    private List<LeaderboardDto.MetricConfig<DailyPlayerPitcher>> getAdvancedPitcherConfigs() {
        return Arrays.asList(
                new LeaderboardDto.MetricConfig<>("퀄리티 스타트 (QS)", "qs", "회", true, 0, false, DailyPlayerPitcher::getQs),
                new LeaderboardDto.MetricConfig<>("피안타율 (AVG)", "avg", "", false, 3, true, DailyPlayerPitcher::getAvg),
                new LeaderboardDto.MetricConfig<>("승률 (WPCT)", "wpct", "", true, 3, true, DailyPlayerPitcher::getWpct),
                new LeaderboardDto.MetricConfig<>("WHIP", "whip", "", false, 2, true, DailyPlayerPitcher::getWhip),
                new LeaderboardDto.MetricConfig<>("GO/AO (땅볼 유도)", "goAo", "", true, 2, true, DailyPlayerPitcher::getGoAo),
                new LeaderboardDto.MetricConfig<>("구원승 (Wgr)", "wgr", "승", true, 0, false, DailyPlayerPitcher::getWgr),
                new LeaderboardDto.MetricConfig<>("BABIP (인플레이 타구 비율)", "babip", "", false, 3, true,
                        p -> {
                            // BABIP = (H - HR) / (BF - SO - BB - HBP - HR)
                            // 필드: h, hr, tbf(BF), so, bb, hbp
                            if (p.getH() == null || p.getHr() == null || p.getTbf() == null
                                    || p.getSo() == null || p.getBb() == null || p.getHbp() == null) {
                                return 999; // null이면 순위 밀려남
                            }
                            double numerator = p.getH() - p.getHr();
                            double denominator = p.getTbf() - p.getSo() - p.getBb() - p.getHbp() - p.getHr();
                            if (denominator <= 0) return 999; // 분모 0 방어
                            return numerator / denominator;
                        }),
                new LeaderboardDto.MetricConfig<>("블론세이브 (BSV)", "bsv", "개", true, 0, false, DailyPlayerPitcher::getBsv)


        );
    }
}

