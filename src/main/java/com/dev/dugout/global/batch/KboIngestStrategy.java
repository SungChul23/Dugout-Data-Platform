package com.dev.dugout.global.batch;

import java.time.LocalDate;


// 4가지 크롤링 전략화
public interface KboIngestStrategy {

    KboDataCategory getCategory();
    void ingest(String s3Path, LocalDate baseDate);
}
