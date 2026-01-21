package com.dev.dugout.domain.user.service;


import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.user.dto.DashboardResponseDto;
import com.dev.dugout.domain.user.dto.PlayerInsightDto;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.entity.UserDashboard;
import com.dev.dugout.domain.user.repository.UserDashboardRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import com.dev.dugout.infrastructure.ml.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserDashboardRepository userDashboardRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDto getUserDashboard(User user) {
        User managedUser = userRepository.findByLoginIdWithTeam(user.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<UserDashboard> userSelections = userDashboardRepository.findByUser(user);
        List<PlayerInsightDto> insights = new ArrayList<>();

        for (int slot = 1; slot <= 3; slot++) {
            final int currentSlot = slot;
            Optional<UserDashboard> selection = userSelections.stream()
                    .filter(d -> d.getSlotNumber() == currentSlot)
                    .findFirst();

            if (selection.isPresent()) {
                Player player = selection.get().getPlayer();

                // 최신 예측 결과 조회
                PredictionResult pred = predictionResultRepository.findTopByPlayerOrderByPredictedAtDesc(player)
                        .orElse(null);

                // DTO 빌더에 새로 만든 컬럼들 적용
                insights.add(PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
                        // 프론트와 식별자 통일을 위해 kboPcode를 숫자로 변환하여 전달
                        .playerId(Long.parseLong(player.getKboPcode()))
                        .name(player.getName())
                        .position(player.getPositionType())
                        // Double(0.0) 대신 BigDecimal.ZERO 사용
                        .predictedAvg(pred != null ? pred.getPredAvg() : BigDecimal.ZERO)
                        .predictedHr(pred != null ? pred.getPredHr() : 0)
                        .predictedOps(pred != null ? pred.getPredOps() : BigDecimal.ZERO)
                        .avgDiff(pred != null ? pred.getAvgDiff() : BigDecimal.ZERO)
                        .hrDiff(pred != null ? pred.getHrDiff() : 0)
                        .opsDiff(pred != null ? pred.getOpsDiff() : BigDecimal.ZERO)
                        .insightSummary(pred != null ? pred.getInsightJson() : "데이터 분석 중")
                        .isEmpty(false)
                        .build());
            } else {
                insights.add(PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
                        .isEmpty(true)
                        .build());
            }
        }
        return new DashboardResponseDto(managedUser.getFavoriteTeam().getName(), managedUser.getFavoriteTeam().getSlogan(), insights);
    }
}