package com.dev.dugout.domain.user.service;


import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.DailyPlayerHitterRepository;
import com.dev.dugout.domain.player.repository.DailyPlayerPitcherRepository;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.dto.NewsItemDto;
import com.dev.dugout.domain.team.dto.NewsResponseDto;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.service.NewsService;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.dto.PlayerInsightDto;
import com.dev.dugout.domain.user.dto.TeamDailyStatsResponseDto;
import com.dev.dugout.domain.user.dto.TeamRankSummaryDto;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.entity.UserDashboard;
import com.dev.dugout.domain.user.repository.UserDashboardRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import com.dev.dugout.infrastructure.ml.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        // 1. 유저 정보 및 선호 팀 정보 조회 (Fetch Join 활용 추천)
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Team team = managedUser.getFavoriteTeam();
        log.info("====> [대시보드 조회] 유저: {}, 선호 팀: {}", managedUser.getLoginId(), team.getName());

        // 2. [추가] 선호 팀의 최신 순위 정보 조회
        // DB에서 가장 최신 날짜를 먼저 찾음 (하드코딩 제거)
        LocalDate latestDate = dailyTeamRankingRepository.findMaxBaseDate()
                .orElse(null);

        TeamRankSummaryDto rankSummary = null;
        if (latestDate != null) {
            // 해당 날짜의 특정 팀 순위 추출
            rankSummary = dailyTeamRankingRepository.findByBaseDateAndTeamId(latestDate, team.getId())
                    .map(ranking -> TeamRankSummaryDto.builder()
                            .teamId(team.getId())
                            .rank(ranking.getRank())
                            .wdl(String.format("%d승-%d무-%d패",
                                    ranking.getWins(), ranking.getDraws(), ranking.getLosses()))
                            .winRate(String.format("%.3f", ranking.getWinRate()))
                            .build())
                    .orElse(null);
        }

        // 3. 뉴스 데이터 (3개 제한)
        List<NewsItemDto> limitedNews = newsService.getKboNews(team.getName())
                .getItems().stream().limit(3).toList();

        // 4. 슬롯별 선수 인사이트 구성
        List<UserDashboard> userSelections = userDashboardRepository.findByUser(managedUser);
        List<PlayerInsightDto> insights = new ArrayList<>();

        for (int slot = 0; slot <= 2; slot++) {
            final int currentSlot = slot;
            Optional<UserDashboard> selection = userSelections.stream()
                    .filter(d -> d.getSlotNumber() == currentSlot).findFirst();

            if (selection.isPresent()) {
                Player player = selection.get().getPlayer();
                PredictionResult pred = predictionResultRepository.findTopByPlayerOrderByPredictedAtDesc(player).orElse(null);

                PlayerInsightDto.PlayerInsightDtoBuilder builder = PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
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
                insights.add(builder.build());
            } else {
                insights.add(PlayerInsightDto.builder().slotNumber(currentSlot).isEmpty(true).build());
            }
        }

        // 5. 최종 대시보드 데이터 빌드
        return DashboardResponseDto.builder()
                .favoriteTeamName(team.getName())
                .teamSlogan(team.getSlogan())
                .bookingUrl(team.getBookingUrl())
                .teamRank(rankSummary) // [수정됨] 실제 DB 데이터 반영
                .insights(insights)
                .news(limitedNews)
                .build();
    }

    // 대시보드 - 선호하는 팀의 선수들 투/타 성적 get
    @Transactional(readOnly = true)
    public TeamDailyStatsResponseDto getTeamDailyStats(User user) {
        // 1. 유저 및 선호 팀 조회
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Team team = managedUser.getFavoriteTeam();

        // 2. 가장 최신 데이터의 날짜 조회 (타자 테이블 기준)
        LocalDate latestDate = dailyPlayerHitterRepository.findMaxBaseDate()
                .orElseThrow(() -> new RuntimeException("일일 성적 데이터가 존재하지 않습니다."));

        // 3. 해당 날짜와 팀 ID로 선수단 데이터 조회
        List<DailyPlayerHitter> hitters = dailyPlayerHitterRepository.findByBaseDateAndTeamId(latestDate, team.getId());
        List<DailyPlayerPitcher> pitchers = dailyPlayerPitcherRepository.findByBaseDateAndTeamId(latestDate, team.getId());

        // 4. 타자 Entity -> DTO 변환
        List<TeamDailyStatsResponseDto.PlayerStatDto> hitterDtos = hitters.stream().map(h -> {
            Map<String, Number> stats = new HashMap<>();
            // Count 지표
            stats.put("h_g", h.getG());
            stats.put("h_pa", h.getPa());
            stats.put("h_ab", h.getAb());
            stats.put("h_r", h.getR());
            stats.put("h_h", h.getH());
            stats.put("h_2b", h.getB2());
            stats.put("h_3b", h.getB3());
            stats.put("h_hr", h.getHr());
            stats.put("h_tb", h.getTb());
            stats.put("h_rbi", h.getRbi());
            stats.put("h_sac", h.getSac());
            stats.put("h_sf", h.getSf());
            stats.put("h_mh", h.getMh());
            stats.put("h_xbh", h.getXbh());
            stats.put("h_go", h.getGo());
            stats.put("h_ao", h.getAo());
            stats.put("h_gw_rbi", h.getGwRbi());
            // Ratio 지표
            stats.put("h_avg", h.getAvg());
            stats.put("h_slg", h.getSlg());
            stats.put("h_obp", h.getObp());
            stats.put("h_ops", h.getOps());
            stats.put("h_risp", h.getRisp());
            stats.put("h_ph_ba", h.getPhBa());
            stats.put("h_bb_k", h.getBbK());
            stats.put("h_isop", h.getIsop());
            stats.put("h_xr", h.getXr());
            stats.put("h_gpa", h.getGpa());

            return TeamDailyStatsResponseDto.PlayerStatDto.builder()
                    .playerId(h.getPlayer().getKboPcode()) // 기존 연관관계된 Player에서 가져옴
                    .playerName(h.getPlayerName())
                    .stats(stats)
                    .build();
        }).collect(Collectors.toList());

        // 5. 투수 Entity -> DTO 변환
        List<TeamDailyStatsResponseDto.PlayerStatDto> pitcherDtos = pitchers.stream().map(p -> {
            Map<String, Number> stats = new HashMap<>();
            // Basic 1
            stats.put("p_era", p.getEra());
            stats.put("p_g", p.getG());
            stats.put("p_w", p.getW());
            stats.put("p_l", p.getL());
            stats.put("p_sv", p.getSv());
            stats.put("p_hld", p.getHld());
            stats.put("p_wpct", p.getWpct());
            stats.put("p_ip", p.getIp());
            stats.put("p_h", p.getH());
            stats.put("p_hr", p.getHr());
            stats.put("p_bb", p.getBb());
            stats.put("p_hbp", p.getHbp());
            stats.put("p_so", p.getSo());
            stats.put("p_r", p.getR());
            stats.put("p_er", p.getEr());
            stats.put("p_whip", p.getWhip());
            // Basic 2
            stats.put("p_cg", p.getCg());
            stats.put("p_sho", p.getSho());
            stats.put("p_qs", p.getQs());
            stats.put("p_bsv", p.getBsv());
            stats.put("p_tbf", p.getTbf());
            stats.put("p_np", p.getNp());
            stats.put("p_avg", p.getAvg());
            stats.put("p_2b", p.getB2());
            stats.put("p_3b", p.getB3());
            stats.put("p_sac", p.getSac());
            stats.put("p_sf", p.getSf());
            stats.put("p_ibb", p.getIbb());
            stats.put("p_wp", p.getWp());
            stats.put("p_bk", p.getBk());
            // Detail 1
            stats.put("p_gs", p.getGs());
            stats.put("p_wgs", p.getWgs());
            stats.put("p_wgr", p.getWgr());
            stats.put("p_gf", p.getGf());
            stats.put("p_svo", p.getSvo());
            stats.put("p_ts", p.getTs());
            stats.put("p_gdp", p.getGdp());
            stats.put("p_go", p.getGo());
            stats.put("p_ao", p.getAo());
            stats.put("p_go_ao", p.getGoAo());

            return TeamDailyStatsResponseDto.PlayerStatDto.builder()
                    .playerId(p.getPlayer().getKboPcode())
                    .playerName(p.getPlayerName())
                    .stats(stats)
                    .build();
        }).collect(Collectors.toList());

        // 6. 응답 객체 반환
        return TeamDailyStatsResponseDto.builder()
                .teamName(team.getName())
                .baseDate(latestDate)
                .hitters(hitterDtos)
                .pitchers(pitcherDtos)
                .build();
    }
}