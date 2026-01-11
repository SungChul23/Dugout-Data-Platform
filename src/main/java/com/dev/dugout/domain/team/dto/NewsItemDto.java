package com.dev.dugout.domain.team.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NewsItemDto {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;
}

