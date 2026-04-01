package com.dev.dugout.infrastructure.aws.batch;

import java.time.LocalDate;

// 4가지 크롤링 전략화
public interface KboIngestStrategy {

    KboDataCategory getCategory();
    void ingest(String s3Path, LocalDate baseDate);

    //  이미 해당 날짜에 데이터가 존재하는지 확인
    boolean isAlreadyIngested(LocalDate baseDate);
}
