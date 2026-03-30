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
public class KboIngestService {

    private final List<KboIngestStrategy> strategyList;

    public void processIngestion(KboIngestRequest request) {
        LocalDate baseDate = LocalDate.parse(request.baseDate());
        log.info(">>>> [KBO 입고 공정] 날짜: {}, 파일 수: {}", baseDate, request.files().size());

        for (KboIngestRequest.FileInfo file : request.files()) {
            try {
                KboDataCategory category = KboDataCategory.valueOf(file.type());

                // 해당 카테고리에 맞는 전략을 찾아서 실행
                strategyList.stream()
                        .filter(strategy -> strategy.getCategory() == category)
                        .findFirst()
                        .ifPresentOrElse(
                                strategy -> {
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