package com.dev.dugout.domain.team.service;


import com.dev.dugout.domain.team.dto.NewsItemDto;
import com.dev.dugout.domain.team.dto.NewsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public NewsResponseDto getKboNews(String team) {
        // 1. 키 주입 여부 확인 (보안을 위해 앞 3글자만 출력)
        log.info("Checking Naver Keys - ID: {}..., Secret: {}...",
                (clientId != null && clientId.length() > 3) ? clientId.substring(0, 3) : "NULL",
                (clientSecret != null && clientSecret.length() > 3) ? clientSecret.substring(0, 3) : "NULL");

        boolean isMlb = "메이저리그 코리안리거".equals(team);
        // 방법 1: 팀명을 따옴표로 감싸 Naver API 레벨에서 정확한 구문 검색
        String searchQuery = isMlb ? "\"MLB 한국인 선수\"" : "\"" + team + "\" 야구";

        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/news.json")
                .queryParam("query", searchQuery)
                .queryParam("display", 15)
                .queryParam("sort", "date")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            log.info("Sending request to Naver API: {}", uri);
            ResponseEntity<NewsResponseDto> response = restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, NewsResponseDto.class
            );
            log.info("Naver API Response Success!");
            NewsResponseDto body = response.getBody();

            // 방법 2: MLB는 팀명 기준 필터링 불필요, KBO 팀은 제목/설명에 팀명 포함된 기사만 반환
            if (body == null || isMlb) return body;

            List<NewsItemDto> filtered = body.getItems().stream()
                    .filter(item -> {
                        String cleanTitle = item.getTitle().replaceAll("<[^>]+>", "");
                        String cleanDesc  = item.getDescription().replaceAll("<[^>]+>", "");
                        return cleanTitle.contains(team) || cleanDesc.contains(team);
                    })
                    .limit(6)
                    .toList();
            log.info("News filtered: {}/{} articles matched team '{}'", filtered.size(), body.getItems().size(), team);
            return new NewsResponseDto(filtered);

        } catch (HttpStatusCodeException e) {
            // 2. 네이버가 던진 실제 에러 본문 확인 (403의 진짜 이유)
            log.error("Naver API Error - Status Code: {}", e.getStatusCode());
            log.error("Naver API Error - Response Body: {}", e.getResponseBodyAsString());
            return new NewsResponseDto(); // 에러 시 빈 객체 반환하여 프론트엔드 차단 방지

        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return new NewsResponseDto();
        }
    }
}
