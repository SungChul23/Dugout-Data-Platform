package com.dev.dugout.domain.team.service;


import com.dev.dugout.domain.team.dto.NewsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    //외부 HTTP API를 호출하기 위한 기본 클라이언트 객체 생성
    private final RestTemplate restTemplate = new RestTemplate();

    public NewsResponseDto getKboNews(String team) {
        // 검색어 보정: 팀 이름만 넣으면 잡음이 많으므로 "야구" 키워드 추가
        String searchQuery = "메이저리그 코리안리거".equals(team) ? "MLB 한국인 선수" : team + " 야구";

        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/news.json")
                .queryParam("query", searchQuery)
                .queryParam("display", 6)   // 6개 고정
                .queryParam("sort", "date")  // 최신순
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        //네이버 API 인증 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            log.debug("네이버 뉴스 API 호출 - 팀: {}, URI: {}", team, uri);

            ResponseEntity<NewsResponseDto> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    NewsResponseDto.class
            );

            return response.getBody();

        } catch (Exception e) {
            // EC2의 app.log에서 확인할 수 있도록 에러 기록
            log.error("네이버 뉴스 API 호출 중 에러 발생 (팀: {}): {}", team, e.getMessage());

            // 에러 발생 시 빈 결과 반환 (프론트에서 '뉴스 존재하지 않음' UI가 뜸)
            return new NewsResponseDto();
        }
    }
}
