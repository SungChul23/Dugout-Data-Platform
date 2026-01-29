package com.dev.dugout.domain.player.service;


import com.dev.dugout.domain.player.dto.FaDetailResponseDto;
import com.dev.dugout.domain.player.dto.FaListResponseDto;
import com.dev.dugout.domain.player.entity.FaMarket;
import com.dev.dugout.domain.player.repository.FaMarketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaMarketService {

    private final FaMarketRepository faMarketRepository;
    private final FaMarketBedrockService faMarketBedrockService;

    // 리스트 조회 (연도별, 팀별 필터링)
    public List<FaListResponseDto> getFaList(Integer year, String teamName) {
        Integer teamId = convertTeamNameToId(teamName);

        return faMarketRepository.findByYearAndTeamIdAndIsFaTargetTrue(year, teamId)
                .stream()
                .map(entity -> FaListResponseDto.builder()
                        .id(entity.getId())
                        .playerName(entity.getPlayerName())
                        .subPositionType(entity.getSubPositionType())
                        .age(entity.getAge())
                        .grade(entity.getGrade())
                        .currentSalary(entity.getCurrentSalary())
                        .playerIntro(entity.getPlayerIntro())
                        .build())
                .collect(Collectors.toList());
    }

    // 상세 조회 (쿼리 파라미터 playerId 기반)
    @Transactional
    public FaDetailResponseDto getFaDetail(Long playerId) {
        FaMarket player = faMarketRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("해당 FA 선수를 찾을 수 없습니다."));

        // aiFeedback이 없으면 베드락 호출
        if (player.getAiFeedback() == null || player.getAiFeedback().isEmpty()) {
            String report = faMarketBedrockService.generateFaReport(player);
            player.setAiFeedback(report);
            faMarketRepository.save(player);
        }

        return FaDetailResponseDto.builder()
                .playerId(player.getId())
                .playerName(player.getPlayerName())
                .subPositionType(player.getSubPositionType())
                .age(player.getAge())
                .playerIntro(player.getPlayerIntro())
                .grade(player.getGrade())
                .currentSalary(player.getCurrentSalary())
                .aiFeedback(player.getAiFeedback())
                .statPitching(player.getStatPitching())
                .statStability(player.getStatStability())
                .statOffense(player.getStatOffense())
                .statDefense(player.getStatDefense())
                .statContribution(player.getStatContribution())
                .build();
    }

    //fa 페이지에서
    private Integer convertTeamNameToId(String name) {
        if (name.contains("삼성")) return 1;
        if (name.contains("두산")) return 2;
        if (name.contains("LG")) return 3;
        if (name.contains("롯데")) return 4;
        if (name.contains("KIA")) return 5;
        if (name.contains("한화")) return 6;
        if (name.contains("SSG")) return 7;
        if (name.contains("키움")) return 8;
        if (name.contains("NC")) return 9;
        if (name.contains("KT") || name.contains("kt")) return 10;
        return 0;
    }
}