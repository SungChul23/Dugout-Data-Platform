package com.dev.dugout.domain.admin.service;

import com.dev.dugout.domain.admin.repository.DataLoading;
import com.dev.dugout.domain.admin.repository.GetPlayerByKboPcode;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionService {

    private final GetPlayerByKboPcode getPlayerByKboPcode;
    private final DataLoading dataLoading;

    @Transactional
    public void initPredictionsFromCsv() throws Exception {
        log.info("2026 시즌 예측 데이터 적재/업데이트 시작....");

        ClassPathResource resource = new ClassPathResource("kbo_2026_batting_MASTER_final.csv");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
            CSVReader csvReader = new CSVReaderBuilder(br).withSkipLines(1).build();
            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                String pcode = line[0];
                String targetSeason = "2026";

                // 1. Player 존재 확인
                Optional<Player> playerOpt = getPlayerByKboPcode.findByKboPcode(pcode);
                if (playerOpt.isPresent()) {
                    Player player = playerOpt.get();

                    // 2. Upsert 로직: 기존 데이터가 있으면 가져오고, 없으면 새로 생성
                    PredictionResult result = dataLoading
                            .findByPlayerAndTargetSeason(player, targetSeason)
                            .orElse(new PredictionResult());

                    result.setPlayer(player);
                    result.setTargetSeason(targetSeason);
                    result.setPredAvg(Double.parseDouble(line[4]));  // pred_avg
                    result.setAvgDiff(Double.parseDouble(line[5]));  // avg_diff
                    result.setPredHr((int)Double.parseDouble(line[7])); // pred_hr
                    result.setHrDiff((int)Double.parseDouble(line[8])); // hr_diff
                    result.setPredOps(Double.parseDouble(line[10])); // pred_ops
                    result.setOpsDiff(Double.parseDouble(line[11])); // ops_diff
                    result.setPredictedAt(LocalDateTime.now());

                    dataLoading.save(result);
                    count++;
                }
            }
            log.info("총 {}명의 데이터가 성공적으로 처리되었습니다.", count);
        }
    }
}
