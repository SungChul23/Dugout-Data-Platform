package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.NewsResponseDto;
import com.dev.dugout.domain.team.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/news")
    public NewsResponseDto getNews(@RequestParam(name = "team") String team){
        return newsService.getKboNews(team);
    }
}
