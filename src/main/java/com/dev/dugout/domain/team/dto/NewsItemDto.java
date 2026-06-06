package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "개별 뉴스 기사 DTO")
@Getter
@NoArgsConstructor
public class NewsItemDto {

    @Schema(description = "뉴스 제목", example = "김도영, 시즌 30호 홈런 달성")
    private String title;

    @Schema(description = "원본 기사 링크")
    private String originallink;

    @Schema(description = "Naver 뉴스 링크")
    private String link;

    @Schema(description = "기사 요약 내용")
    private String description;

    @Schema(description = "기사 발행일 (RFC 822 형식)", example = "Fri, 06 Jun 2026 18:00:00 +0900")
    private String pubDate;
}
