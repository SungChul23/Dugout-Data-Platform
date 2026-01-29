package com.dev.dugout.domain.player.controller;


import com.dev.dugout.domain.player.dto.FaDetailResponseDto;
import com.dev.dugout.domain.player.dto.FaListResponseDto;
import com.dev.dugout.domain.player.service.FaMarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fa-market")
@RequiredArgsConstructor
public class FaMarketController {


    private final FaMarketService faMarketService;

    //fa 명단 가져오기
    @GetMapping("/list")
    public ResponseEntity<List<FaListResponseDto>> getList(
            @RequestParam("year") Integer year,
            @RequestParam("team") String team) {
        return ResponseEntity.ok(faMarketService.getFaList(year, team));
    }

    //fa 선수 상세 조회 (스텟 + 베드락 피드백)
    @GetMapping("/detail")
    public ResponseEntity<FaDetailResponseDto> getDetail(
            @RequestParam("playerId") Long playerId) {
        return ResponseEntity.ok(faMarketService.getFaDetail(playerId));
    }
}
