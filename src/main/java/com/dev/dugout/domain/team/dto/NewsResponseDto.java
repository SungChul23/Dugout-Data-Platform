package com.dev.dugout.domain.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "팀별 KBO 뉴스 응답 DTO")
@Getter
@NoArgsConstructor
public class NewsResponseDto {

    @Schema(description = "뉴스 기사 목록")
    private List<NewsItemDto> items = new ArrayList<>();
}
