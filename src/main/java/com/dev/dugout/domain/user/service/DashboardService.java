package com.dev.dugout.domain.user.service;


import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.dto.NewsItemDto;
import com.dev.dugout.domain.team.dto.NewsResponseDto;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.service.NewsService;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.dto.PlayerInsightDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<UserDashboard> userSelections = userDashboardRepository.findByUser(managedUser);
        log.info("====> [DB 조회 결과] 유저: {}, 찾은 데이터 개수: {}", managedUser.getLoginId(), userSelections.size());
        List<PlayerInsightDto> insights = new ArrayList<>();
        Team team = managedUser.getFavoriteTeam();

        // 뉴스 데이터 3개 제한
        List<NewsItemDto> limitedNews = newsService.getKboNews(team.getName())
                .getItems().stream().limit(3).toList();

        // 1~3번 슬롯 구성
        for (int slot = 0; slot <= 2; slot++) {
            final int currentSlot = slot;
            Optional<UserDashboard> selection = userSelections.stream()
                    .filter(d -> d.getSlotNumber() == currentSlot).findFirst();

            if (selection.isPresent()) {
                log.info("====> [대시보드 확인] 슬롯: {}, 선수명: {}", currentSlot, selection.get().getPlayer().getName());
            } else {
                log.info("====> [대시보드 확인] 슬롯: {} 은 비어있음", currentSlot);
            }

            if (selection.isPresent()) {
                Player player = selection.get().getPlayer();
                // 최신 예측 결과 조회
                PredictionResult pred = predictionResultRepository.findTopByPlayerOrderByPredictedAtDesc(player).orElse(null);

                PlayerInsightDto.PlayerInsightDtoBuilder builder = PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
                        .playerId(Long.parseLong(player.getKboPcode()))
                        .name(player.getName())
                        .backNumber(player.getBackNumber())
                        .position(player.getPositionType())
                        // player 엔티티를 통해 팀 코드 직접 추출
                        .teamCode(player.getTeam() != null ? String.valueOf(player.getTeam().getId()) : null)
                        .isEmpty(false);

                // 투수/타자 지표 분기 매핑
                if ("투수".equals(player.getPositionType()) && pred != null) {
                    builder.probElite(pred.getProbElite())
                            .rolePercentileTop(pred.getRolePercentileTop())
                            .roleRank(pred.getRoleRank())
                            .roleTotal(pred.getRoleTotal());
                } else if (pred != null) {
                    builder.predictedAvg(pred.getPredAvg())
                            .predictedHr(pred.getPredHr())
                            .predictedOps(pred.getPredOps())
                            .avgDiff(pred.getAvgDiff())
                            .hrDiff(pred.getHrDiff())
                            .opsDiff(pred.getOpsDiff());
                }
                insights.add(builder.build());
            } else {
                insights.add(PlayerInsightDto.builder().slotNumber(currentSlot).isEmpty(true).build());
            }
        }

        return DashboardResponseDto.builder()
                .favoriteTeamName(team.getName())
                .teamSlogan(team.getSlogan())
                .bookingUrl(team.getBookingUrl())
                .insights(insights)
                .news(limitedNews)
                .build();
    }

}