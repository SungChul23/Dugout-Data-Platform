package com.dev.dugout.domain.player.service;

import com.dev.dugout.domain.player.dto.PlayerResponseDto;
import com.dev.dugout.domain.player.dto.PredictionResponseDto;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import com.dev.dugout.infrastructure.ml.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PlayerRepository playerRepository;
    private final PredictionResultRepository predictionRepository;
    private final ReportBedrockService reportBedrockService;

    //구단별/타입별(타자/투수) 선수 명단 조회
    public List<PlayerResponseDto> getRoster(String teamName, String type) {
        log.info("====> [로스터 조회 시작] 팀명: {}, 타입: {}", teamName, type);

        List<Player> players = "pitcher".equalsIgnoreCase(type)
                ? playerRepository.findPredictablePitchers(teamName)
                : playerRepository.findPredictableHitters(teamName);

        return players.stream()
                .map(p -> PlayerResponseDto.builder()
                        .playerId(Long.valueOf(p.getKboPcode()))
                        .name(p.getName())
                        .backNumber(p.getBackNumber())
                        .positionType(p.getPositionType())
                        .build())
                .toList();
    }

    //상세 분석 결과 조회 (투수/타자 자동 판별)
    @Transactional
    public PredictionResponseDto getAnalysis(Long kboPcode) {
        log.info("====> [상세 분석 시작] KBO PCODE: {}", kboPcode);

        // 1. 선수 엔티티 조회
        Player player = playerRepository.findByKboPcode(String.valueOf(kboPcode))
                .orElseThrow(() -> new RuntimeException("해당 선수를 찾을 수 없습니다."));

        // 2. 최신 예측 결과 조회
        PredictionResult pred = predictionRepository.findTopByPlayerOrderByPredictedAtDesc(player)
                .orElseThrow(() -> new RuntimeException("예측 데이터가 존재하지 않습니다."));

        // 3. AI 리포트 캐싱 로직 (포지션별 프롬프트는 BedrockService 내부에서 처리됨)
        String report = pred.getInsightJson();
        if (report == null || report.isBlank()) {
            log.info("====> [AI 리포트 생성 중] 대상: {}", player.getName());
            report = reportBedrockService.generatePlayerReport(pred);
            pred.setInsightJson(report);
            predictionRepository.save(pred);
        }

        // 4. 응답 DTO 구성 (포지션별 분기 처리)
        PredictionResponseDto.PredictionResponseDtoBuilder builder = PredictionResponseDto.builder()
                .name(player.getName())
                .backNumber(player.getBackNumber())
                .position(player.getPositionType())
                .aiReport(report);

        if ("투수".equals(player.getPositionType())) {
            log.info("====> [투수 데이터 구성] 선수: {}", player.getName());
            // 투수는 현재 성적(ERA, WHIP)과 엘리트 확률 정보를 전달
            builder.currentEra(pred.getPredEra())
                    .currentWhip(pred.getPredWhip())
                    .eraEliteProb(pred.getEraEliteProb())
                    .whipEliteProb(pred.getWhipEliteProb());
        } else {
            log.info("====> [타자 데이터 구성] 선수: {}", player.getName());
            // 타자는 기존대로 예측치와 변화량을 계산하여 전달
            BigDecimal currentAvg = pred.getPredAvg()
                    .subtract(pred.getAvgDiff() != null ? pred.getAvgDiff() : BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal currentOps = pred.getPredOps()
                    .subtract(pred.getOpsDiff() != null ? pred.getOpsDiff() : BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);

            Integer currentHr = pred.getPredHr() - (pred.getHrDiff() != null ? pred.getHrDiff() : 0);

            builder.predAvg(pred.getPredAvg()).predHr(pred.getPredHr()).predOps(pred.getPredOps())
                    .avgDiff(pred.getAvgDiff()).hrDiff(pred.getHrDiff()).opsDiff(pred.getOpsDiff())
                    .currentAvg(currentAvg).currentHr(currentHr).currentOps(currentOps);
        }
        return builder.build();
    }
}