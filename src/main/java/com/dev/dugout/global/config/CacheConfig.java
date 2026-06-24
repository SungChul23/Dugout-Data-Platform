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

    /**
     * Caffeine 캐시 매니저.
     * 
     * 캐시 TTL 전략:
     * - 기본 TTL: 6시간 (야구 데이터는 하루 1회 적재 기준)
     * - 적재 API 호출 시 @CacheEvict로 강제 무효화
     * - maximumSize: 200 (팀별/포지션별 캐시 키 증가 대응)
     *
     * 적용 대상:
     * - teamRanking: 팀 순위 (6h, evict: /ingest/kbo/notify)
     * - teamPerformance: 팀 성적 (6h, evict: /ingest/kbo/notify)
     * - hitterLeaderboard: 타자 리더보드 (6h, evict: /ingest/kbo/notify)
     * - pitcherLeaderboard: 투수 리더보드 (6h, evict: /ingest/kbo/notify)
     * - goldenGlove: 골든글러브 예측 (6h, evict: /ingest/ml/gg)
     * - faMarketList: FA 선수 목록 (6h, evict: 거의 없음)
     * - dashboardStats: 대시보드 일일 성적 (6h, evict: /ingest/kbo/notify)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "teamRanking",
                "teamPerformance",
                "hitterLeaderboard",
                "pitcherLeaderboard",
                "goldenGlove",
                "faMarketList",
                "dashboardStats"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(200));
        return cacheManager;
    }
}
