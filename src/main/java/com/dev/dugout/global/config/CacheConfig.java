package com.dev.dugout.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    //스프링 부트 메모리에 결과를 저장할 수 있도록 Caffeine 캐시를 활성화 (선수들 각 지표별 top 5 리더보드에 사용하기 위함)
    // 단 챗봇 방식인 경우엔 대화 히스토리를 conversationId(대화 아이디) 별로 직접 관리해야 직접 제어할 예정
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES) // 10분 뒤 자동 만료 (DB 부하 제로)
                .maximumSize(100)); // 최대 100개 항목 유지
        return cacheManager;
    }
}
