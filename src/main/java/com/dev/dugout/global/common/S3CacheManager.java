package com.dev.dugout.global.common;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * S3에서 로드한 JSON 데이터를 메모리에 캐싱하고 키 기반으로 조회하는 공통 컴포넌트.
 * FaMarketBedrockService.init()과 ReportBedrockService.init()의 중복 캐싱 패턴을 통합한다.
 */
@Component
@Slf4j
public class S3CacheManager {

    private final Map<String, Map<String, String>> cacheStore = new ConcurrentHashMap<>();

    /**
     * JSON 배열 형태의 데이터를 파싱하여 캐시에 로드한다.
     *
     * @param cacheKey     캐시 식별 키 (예: "fa-master", "report-master")
     * @param jsonContent  S3에서 읽은 JSON 문자열 (JSON Array 형태)
     * @param keyExtractor JSON 배열의 각 요소에서 캐시 키를 추출하는 함수
     * @param label        로깅용 라벨 (예: "FA 타자", "투수")
     * @return 로드된 항목 수
     */
    public int load(String cacheKey, String jsonContent,
                    Function<JSONObject, String> keyExtractor, String label) {
        if (jsonContent == null) {
            log.warn("[S3CacheManager] {} 데이터가 null입니다. 빈 캐시로 유지합니다.", label);
            return 0;
        }

        try {
            JSONArray jsonArray = new JSONArray(jsonContent);
            Map<String, String> dataMap = cacheStore.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>());

            int count = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String key = keyExtractor.apply(obj);
                if (key != null && !key.isBlank()) {
                    dataMap.put(key, obj.toString());
                    count++;
                }
            }

            log.info("[S3CacheManager] {} 명의 {} 데이터를 캐싱했습니다. (cacheKey: {})",
                    count, label, cacheKey);
            return count;

        } catch (Exception e) {
            log.error("[S3CacheManager] {} 데이터 로드 실패: {}", label, e.getMessage());
            return 0;
        }
    }

    /**
     * 캐시에서 개별 항목을 조회한다.
     *
     * @param cacheKey 캐시 식별 키
     * @param itemKey  항목 키 (예: pcode)
     * @return 캐시된 JSON 문자열, 없으면 Optional.empty()
     */
    public Optional<String> get(String cacheKey, String itemKey) {
        Map<String, String> dataMap = cacheStore.get(cacheKey);
        if (dataMap == null) return Optional.empty();
        return Optional.ofNullable(dataMap.get(itemKey));
    }

    /**
     * 캐시에서 개별 항목을 조회하고, 없으면 기본값을 반환한다.
     *
     * @param cacheKey     캐시 식별 키
     * @param itemKey      항목 키
     * @param defaultValue 캐시 미스 시 반환할 기본값
     * @return 캐시된 값 또는 기본값
     */
    public String getOrDefault(String cacheKey, String itemKey, String defaultValue) {
        return get(cacheKey, itemKey).orElse(defaultValue);
    }

    /**
     * 특정 캐시를 갱신한다. 갱신 실패 시 기존 데이터를 유지한다.
     */
    public void refresh(String cacheKey, String jsonContent,
                        Function<JSONObject, String> keyExtractor, String label) {
        if (jsonContent == null) {
            log.warn("[S3CacheManager] refresh 실패 - {} 데이터가 null. 기존 캐시 유지.", label);
            return;
        }

        try {
            Map<String, String> newData = new ConcurrentHashMap<>();
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String key = keyExtractor.apply(obj);
                if (key != null && !key.isBlank()) {
                    newData.put(key, obj.toString());
                }
            }

            cacheStore.put(cacheKey, newData);
            log.info("[S3CacheManager] {} 캐시 갱신 완료. ({}건)", label, newData.size());

        } catch (Exception e) {
            log.error("[S3CacheManager] {} 캐시 갱신 실패. 기존 데이터 유지. error: {}", label, e.getMessage());
        }
    }

    /**
     * 캐시 키 존재 여부를 확인한다.
     */
    public boolean containsCache(String cacheKey) {
        return cacheStore.containsKey(cacheKey) && !cacheStore.get(cacheKey).isEmpty();
    }
}
