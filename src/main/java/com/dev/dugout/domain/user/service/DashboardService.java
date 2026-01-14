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

        //팀 정보까지 꽉 채워서 -> Detached 해결
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

                // Optional에서 값을 안전하게 꺼냄
                PredictionResult pred = predictionResultRepository.findTopByPlayerOrderByPredictedAtDesc(player)
                        .orElse(null); // 데이터가 없으면 null을 반환하도록 설정

                insights.add(PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
                        .playerId(player.getPlayerId())
                        .name(player.getName())
                        .position(player.getPositionType())
                        // pred가 null일 경우를 대비한 삼항 연산자 처리
                        .predictedStat(pred != null ? pred.getPredictionData() : "예측 데이터 생성 중")
                        .confidence(pred != null ? pred.getConfidence() : 0)
                        .isEmpty(false)
                        .build());
            } else {
                // 뉴비나 빈 슬롯일 때
                insights.add(PlayerInsightDto.builder()
                        .slotNumber(currentSlot)
                        .isEmpty(true)
                        .build());
            }
        }
        return new DashboardResponseDto(managedUser.getFavoriteTeam().getName(), insights);
    }
}
