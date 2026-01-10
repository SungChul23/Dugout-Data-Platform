package com.dev.dugout.domain.team.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class NewsResponseDto {
    private List<NewsItem> items = new ArrayList<>(); // null 대신 빈 리스트로 초기화
}
