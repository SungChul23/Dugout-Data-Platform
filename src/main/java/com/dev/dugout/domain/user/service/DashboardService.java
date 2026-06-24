package com.dev.dugout.domain.user.service;


import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.DailyPlayerHitterRepository;
import com.dev.dugout.domain.player.repository.DailyPlayerPitcherRepository;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.dto.NewsItemDto;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.service.NewsService;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.dto.PlayerInsightDto;
import com.dev.dugout.domain.user.dto.TeamDailyStatsResponseDto;
import com.dev.dugout.domain.user.dto.TeamRankSummaryDto;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.entity.UserDashboard;
import com.dev.dugout.domain.user.mapper.HitterStatsMapper;
import com.dev.dugout.domain.user.mapper.PitcherStatsMapper;
import com.dev.dugout.domain.user.repository.UserDashboardRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import com.dev.dugout.infrastructure.ml.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserDashboardRepository userDashboardRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UserRepository userRepository;
    private final NewsService newsService; // 기존 뉴스 서비스 디펜던시 인젝션 주입
    private final PlayerRepository playerRepository;
    private final DailyTeamRankingRepository dailyTeamRankingRepository;

    //사용자가 선호 하는 팀의 선수 , 투/타 기록 가져오기
    private final DailyPlayerHitterRepository dailyPlayerHitterRepository;
    private final DailyPlayerPitcherRepository dailyPlayerPitcherRepository;

    //대시보드 선수 추가
    @Transactional
    public void addPlayer(User user, Long kboPcode) {
        // 1. 현재 유저가 대시보드에 등록한 리스트를 가져옴
        List<UserDashboard> currentSelections = userDashboardRepository.findByUser(user);

        // 1.5 입력받은 kboPcode가 이미 리스트에 있는지 확인
        boolean isDuplicate = currentSelections.stream()
                .anyMatch(d -> d.getPlayer().getKboPcode().equals(String.valueOf(kboPcode)));

        if (isDuplicate) {
            throw new RuntimeException("이미 대시보드에 등록된 선수입니다.");
        }

        // 2. 최대 3개까지만 허용
        if (currentSelections.size() >= 3) {
            throw new RuntimeException("대시보드가 가득 찼습니다. 기존 선수를 제거하고 추가해 주세요.");
        }

        // 3. 빈 슬롯 찾기 (1, 2, 3번 중 없는 번호 찾기)
        Set<Integer> occupiedSlots = currentSelections.stream()
                .map(UserDashboard::getSlotNumber)
                .collect(Collectors.toSet());

        int targetSlot = -1;
        for (int i = 0; i <= 2; i++) {
            if (!occupiedSlots.contains(i)) {
                targetSlot = i;
                break;
            }
        }

        // 4. 선수 조회 및 저장
        Player player = playerRepository.findByKboPcode(String.valueOf(kboPcode))
                .orElseThrow(() -> new RuntimeException("선수를 찾을 수 없습니다."));

        userDashboardRepository.save(UserDashboard.builder()
                .user(user)
                .player(player)
                .slotNumber(targetSlot)
                .build());
    }

    //대시보드 선수 삭제
    @Transactional
    public void removePlayer(User user, int slotNumber) {
        userDashboardRepository.deleteByUserAndSlotNumber(user, slotNumber);
    }

    @Transactional(readOnly = true)
    public DashboardResponseDto getUserDashboard(User user) {
        // 1. 유저 정보 및 선호 팀 조회
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Team team = managedUser.getFavoriteTeam();
        log.info("====> [대시보드 조회] 유저: {}, 선호 팀: {}", managedUser.getLoginId(), team.getName());

        // 2. 분리된 메서드 호출로 데이터 구성
        TeamRankSummaryDto rankSummary = fetchTeamRank(team);
        List<NewsItemDto> limitedNews = fetchLimitedNews(team);
        List<PlayerInsightDto> insights = buildPlayerInsights(
                userDashboardRepository.findByUser(managedUser));

        // 3. 최종 대시보드 빌드
        return DashboardResponseDto.builder()
                .favoriteTeamName(team.getName())
                .teamSlogan(team.getSlogan())
                .bookingUrl(team.getBookingUrl())
                .teamRank(rankSummary)
                .insights(insights)
                .news(limitedNews)
                .build();
    }

    /**
     * 선호 팀의 최신 순위 정보를 조회한다.
     */
    private TeamRankSummaryDto fetchTeamRank(Team team) {
        LocalDate latestDate = dailyTeamRankingRepository.findMaxBaseDate().orElse(null);
        if (latestDate == null) return null;

        return dailyTeamRankingRepository.findByBaseDateAndTeamId(latestDate, team.getId())
                .map(ranking -> TeamRankSummaryDto.builder()
                        .teamId(team.getId())
                        .rank(ranking.getRank())
                        .wdl(String.format("%d승-%d무-%d패",
                                ranking.getWins(), ranking.getDraws(), ranking.getLosses()))
                        .winRate(String.format("%.3f", ranking.getWinRate()))
                        .build())
                .orElse(null);
    }

    /**
     * 선호 팀의 최신 뉴스 3건을 조회한다.
     */
    private List<NewsItemDto> fetchLimitedNews(Team team) {
        return newsService.getKboNews(team.getName())
                .getItems().stream().limit(3).toList();
    }

    /**
     * 유저가 등록한 슬롯별 선수 인사이트를 구성한다.
     */
    private List<PlayerInsightDto> buildPlayerInsights(List<UserDashboard> userSelections) {
        List<PlayerInsightDto> insights = new ArrayList<>();

        for (int slot = 0; slot <= 2; slot++) {
            final int currentSlot = slot;
            Optional<UserDashboard> selection = userSelections.stream()
                    .filter(d -> d.getSlotNumber() == currentSlot).findFirst();

            if (selection.isPresent()) {
                insights.add(buildSingleInsight(selection.get(), currentSlot));
            } else {
                insights.add(PlayerInsightDto.builder().slotNumber(currentSlot).isEmpty(true).build());
            }
        }
        return insights;
    }

    /**
     * 단일 슬롯의 선수 인사이트를 구성한다.
     */
    private PlayerInsightDto buildSingleInsight(UserDashboard dashboard, int slot) {
        Player player = dashboard.getPlayer();
        PredictionResult pred = predictionResultRepository
                .findTopByPlayerOrderByPredictedAtDesc(player).orElse(null);

        PlayerInsightDto.PlayerInsightDtoBuilder builder = PlayerInsightDto.builder()
                .slotNumber(slot)
                .playerId(Long.parseLong(player.getKboPcode()))
                .name(player.getName())
                .backNumber(player.getBackNumber())
                .position(player.getPositionType())
                .teamCode(player.getTeam() != null ? String.valueOf(player.getTeam().getId()) : null)
                .isEmpty(false);

        if ("투수".equals(player.getPositionType()) && pred != null) {
            builder.probElite(pred.getProbElite()).rolePercentileTop(pred.getRolePercentileTop())
                    .roleRank(pred.getRoleRank()).roleTotal(pred.getRoleTotal());
        } else if (pred != null) {
            builder.predictedAvg(pred.getPredAvg()).predictedHr(pred.getPredHr()).predictedOps(pred.getPredOps())
                    .avgDiff(pred.getAvgDiff()).hrDiff(pred.getHrDiff()).opsDiff(pred.getOpsDiff());
        }
        return builder.build();
    }

    // 대시보드 - 선호하는 팀의 선수들 투/타 성적 get
    @Transactional(readOnly = true)
    public TeamDailyStatsResponseDto getTeamDailyStats(User user) {
        // 1. 유저 및 선호 팀 조회
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Team team = managedUser.getFavoriteTeam();

        // 2. 가장 최신 데이터의 날짜 조회
        LocalDate latestDate = dailyPlayerHitterRepository.findMaxBaseDate()
                .orElseThrow(() -> new RuntimeException("일일 성적 데이터가 존재하지 않습니다."));

        // 3. 해당 날짜와 팀 ID로 선수단 데이터 조회
        List<DailyPlayerHitter> hitters = dailyPlayerHitterRepository.findByBaseDateAndTeamId(latestDate, team.getId());
        List<DailyPlayerPitcher> pitchers = dailyPlayerPitcherRepository.findByBaseDateAndTeamId(latestDate, team.getId());

        // 4. 매퍼를 통한 Entity → DTO 변환
        List<TeamDailyStatsResponseDto.PlayerStatDto> hitterDtos = hitters.stream()
                .map(HitterStatsMapper::toDto)
                .collect(Collectors.toList());

        List<TeamDailyStatsResponseDto.PlayerStatDto> pitcherDtos = pitchers.stream()
                .map(PitcherStatsMapper::toDto)
                .collect(Collectors.toList());

        // 5. 응답 객체 반환
        return TeamDailyStatsResponseDto.builder()
                .teamName(team.getName())
                .baseDate(latestDate)
                .hitters(hitterDtos)
                .pitchers(pitcherDtos)
                .build();
    }
}