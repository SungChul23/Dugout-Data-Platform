package com.dev.dugout.infrastructure.aws.service;


import com.dev.dugout.infrastructure.aws.batch.KboDataCategory;
import com.dev.dugout.infrastructure.aws.batch.KboIngestStrategy;
import com.dev.dugout.infrastructure.aws.dto.KboIngestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KBODataIngestService {

    private final List<KboIngestStrategy> strategyList;

    public void processIngestion(KboIngestRequest request) {
        LocalDate baseDate = LocalDate.parse(request.baseDate());
        log.info(">>>> [KBO 입고 공정] 날짜: {}, 파일 수: {}", baseDate, request.files().size());

        for (KboIngestRequest.FileInfo file : request.files()) {
            try {
                KboDataCategory category = KboDataCategory.valueOf(file.type());

                strategyList.stream()
                        .filter(strategy -> strategy.getCategory() == category)
                        .findFirst()
                        .ifPresentOrElse(
                                strategy -> {
                                    // 무조건 건너뛰는 대신, 상황에 맞게 처리
                                    if (strategy.isAlreadyIngested(baseDate)) {
                                        log.info(">>>> [{}] 이미 데이터가 존재하여 업데이트(재입고)를 진행합니다. 날짜: {}", category, baseDate);
                                        // 필요하다면 여기서 strategy.deleteExisting(baseDate) 호출
                                    }

                                    log.info(">>>> [{}] 입고 시작: {}", category, file.s3Path());
                                    strategy.ingest(file.s3Path(), baseDate);
                                },
                                () -> log.warn(">>>> [건너뜀] 매칭되는 전략이 없음: {}", category)
                        );
            } catch (Exception e) {
                log.error(">>>> [공정 실패] 파일: {}, 사유: {}", file.s3Path(), e.getMessage());
            }
        }
        log.info(">>>> [KBO 입고 공정 완료]");
    }
}