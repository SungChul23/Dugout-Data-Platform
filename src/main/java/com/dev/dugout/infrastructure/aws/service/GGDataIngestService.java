package com.dev.dugout.infrastructure.aws.service;


import com.dev.dugout.domain.player.entity.GoldenGlovePrediction;
import com.dev.dugout.domain.player.repository.GoldenGlovePredictionRepository;
import com.dev.dugout.infrastructure.aws.dto.GoldenGloveRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GGDataIngestService {

    private final GoldenGlovePredictionRepository repository;

    @Transactional
    public void ingestPredictions(GoldenGloveRequestDto.IngestRequest request) {
        LocalDate baseDate = LocalDate.parse(request.getBaseDate());

        // 1. 기존 데이터 삭제 (스텝 함수 재실행 시 중복 방지)
        repository.deleteByBaseDate(baseDate);
        log.info("기존 데이터 삭제 완료: {}", baseDate);

        // 2. DTO -> Entity 변환
        List<GoldenGlovePrediction> entities = request.getPredictions().stream()
                .map(dto -> GoldenGlovePrediction.builder()
                        .baseDate(baseDate)
                        .playerCode(dto.getPlayerCode())
                        .playerName(dto.getPlayerName())
                        .teamName(dto.getTeamName())
                        .position(dto.getPosition())
                        .winProb(dto.getWinProb())
                        .winProbStr(dto.getWinProbStr())
                        .rank(dto.getRank())
                        .top3Positive(dto.getTop3Positive())
                        .top1Negative(dto.getTop1Negative())
                        .aiExplanation(dto.getAiExplanation())
                        .build())
                .collect(Collectors.toList());

        // 3. 일괄 저장 (Bulk Insert)
        repository.saveAll(entities);
        log.info("새로운 골든글러브 데이터 저장 완료. 총 {}건", entities.size());
    }
}