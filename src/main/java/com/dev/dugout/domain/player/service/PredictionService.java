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

    //구단별 선수 명단 조회
    public List<PlayerResponseDto> getRoster(String teamName) {
        log.info("====> [로스터 조회 시작] 팀명: {}", teamName);

        return playerRepository.findAllByTeamNameAndPositionTypeNot(teamName, "투수")
                .stream()
                .map(p -> PlayerResponseDto.builder()
                        // kboPcode(String)를 DTO의 Long 타입으로 변환하여 전달
                        .playerId(Long.valueOf(p.getKboPcode()))
                        .name(p.getName())
                        .backNumber(p.getBackNumber())
                        .positionType(p.getPositionType())
                        .build())
                .toList();
    }

    //상세 분석 결과 조회
    @Transactional
    public PredictionResponseDto getAnalysis(Long kboPcode) {
        log.info("====> [상세 분석 시작] KBO PCODE: {}", kboPcode);

        // 1. pcode로 Player 엔티티 조회 (long->String 형변환)
        Player player = playerRepository.findByKboPcode(String.valueOf(kboPcode))
                .orElseThrow(() -> {
                    log.error("#### [에러] 선수를 찾을 수 없음. PCODE: {}", kboPcode);
                    return new RuntimeException("해당 선수를 찾을 수 없습니다.");
                });

        // 2. Player 엔티티로 최신 예측 결과 조회 (성철님이 제안하신 메서드 활용)
        PredictionResult pred = predictionRepository.findTopByPlayerOrderByPredictedAtDesc(player)
                .orElseThrow(() -> {
                    log.error("#### [에러] 예측 데이터 없음. 선수명: {}", player.getName());
                    return new RuntimeException("예측 데이터가 존재하지 않습니다.");
                });

        // 3. 현재 성적 계산 (BigDecimal subtract 연산으로 정밀도 유지)
        BigDecimal currentAvg = pred.getPredAvg()
                .subtract(pred.getAvgDiff())
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal currentOps = pred.getPredOps()
                .subtract(pred.getOpsDiff())
                .setScale(3, RoundingMode.HALF_UP);

        // 홈런은 정수 연산
        Integer currentHr = pred.getPredHr() - pred.getHrDiff();

        // 4. AI 리포트 캐싱 및 생성 로직
        String report = pred.getInsightJson();
        if (report == null || report.isBlank()) {
            log.info("====> [AI 리포트 생성 중] 캐시가 없어 Bedrock을 호출합니다. 대상: {}", player.getName());

            long startTime = System.currentTimeMillis();
            // ReportBedrockService 내부에서 pred.getPlayer().getKboPcode()를 사용하여 S3 Map을 조회합니다.
            report = reportBedrockService.generatePlayerReport(pred);
            long endTime = System.currentTimeMillis();

            log.info("====> [AI 리포트 생성 완료] 소요 시간: {}ms", (endTime - startTime));

            pred.setInsightJson(report);
            predictionRepository.save(pred);
        } else {
            log.info("====> [AI 리포트 캐시 사용] 기존 리포트를 반환합니다.");
        }

        log.info("====> [분석 완료] 최종 결과 응답 구성. 선수: {}", player.getName());

        return PredictionResponseDto.builder()
                .name(player.getName())
                .backNumber(player.getBackNumber())
                .position(player.getPositionType())
                .predAvg(pred.getPredAvg())
                .predHr(pred.getPredHr())
                .predOps(pred.getPredOps())
                .avgDiff(pred.getAvgDiff())
                .hrDiff(pred.getHrDiff())
                .opsDiff(pred.getOpsDiff())
                .currentAvg(currentAvg)
                .currentHr(currentHr)
                .currentOps(currentOps)
                .aiReport(report)
                .build();
    }
}