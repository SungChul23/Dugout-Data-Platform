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
            builder.pitcherStats(PredictionResponseDto.PitcherStats.builder()
                    .probElite(pred.getProbElite())
                    .rolePercentileTop(pred.getRolePercentileTop())
                    .roleRank(pred.getRoleRank())
                    .roleTotal(pred.getRoleTotal())
                    .era2025(pred.getEra2025())
                    .fip2025(pred.getFip2025())
                    .ip2025(pred.getIp2025())
                    .whip2025(pred.getWhip2025())
                    .role(pred.getRole())
                    .build());
        } else {
            log.info("====> [타자 데이터 구성] 선수: {}", player.getName());
            builder.hitterStats(PredictionResponseDto.HitterStats.builder()
                    .currAvg(pred.getCurrAvg()).predAvg(pred.getPredAvg()).avgDiff(pred.getAvgDiff())
                    .avgMin(pred.getAvgMin()).avgMax(pred.getAvgMax())
                    .currObp(pred.getCurrObp()).predObp(pred.getPredObp()).diffObp(pred.getDiffObp())
                    .obpMin(pred.getObpMin()).obpMax(pred.getObpMax())
                    .currSlg(pred.getCurrSlg()).predSlg(pred.getPredSlg()).diffSlg(pred.getDiffSlg())
                    .slgMin(pred.getSlgMin()).slgMax(pred.getSlgMax())
                    .currOps(pred.getCurrOps()).predOps(pred.getPredOps()).opsDiff(pred.getOpsDiff())
                    .opsMin(pred.getOpsMin()).opsMax(pred.getOpsMax())
                    .currHr(pred.getCurrHr()).predHr(pred.getPredHr()).hrDiff(pred.getHrDiff())
                    .hrMin(pred.getHrMin()).hrMax(pred.getHrMax())
                    .build());
        }
        return builder.build();
    }
}