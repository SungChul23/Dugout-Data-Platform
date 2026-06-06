package com.dev.dugout.domain.team.controller;

import com.dev.dugout.domain.team.dto.NewsResponseDto;
import com.dev.dugout.domain.team.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team", description = "팀·경기·뉴스·티켓 관련 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 팀명 파라미터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 Naver API 오류")
})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NewsController {

    private final NewsService newsService;

    @Operation(
            summary = "팀별 KBO 뉴스 조회",
            description = "Naver 뉴스 API를 통해 특정 구단의 최신 뉴스를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "뉴스 목록 반환 성공")
    @GetMapping("/news")
    public NewsResponseDto getNews(
            @Parameter(description = "구단명 키워드 (한글)", example = "KIA 타이거즈", required = true)
            @RequestParam(name = "team") String team) {
        return newsService.getKboNews(team);
    }
}
