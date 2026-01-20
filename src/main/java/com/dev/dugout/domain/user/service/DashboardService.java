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
                        .playerId(player.getPlayerId())
                        .name(player.getName())
                        .position(player.getPositionType())
                        // 이제 JSON 전체가 아니라 필요한 숫자 데이터를 직접 매핑 가능!
                        .predictedAvg(pred != null ? pred.getPredAvg() : 0.0)
                        .predictedHr(pred != null ? pred.getPredHr() : 0)
                        .predictedOps(pred != null ? pred.getPredOps() : 0.0)
                        // 차이값(diff)도 프론트에서 화살표 표시할 때 필요함
                        .avgDiff(pred != null ? pred.getAvgDiff() : 0.0)
                        .hrDiff(pred != null ? pred.getHrDiff() : 0)
                        .opsDiff(pred != null ? pred.getOpsDiff() : 0.0)
                        // 상세 요약이 필요할 때만 JSON 사용
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