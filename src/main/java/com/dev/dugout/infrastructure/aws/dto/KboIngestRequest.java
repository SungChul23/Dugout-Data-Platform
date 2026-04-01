package com.dev.dugout.infrastructure.aws.dto;

import java.util.List;

//AWS StepFunction이 스프링 서버에게 보내는 "오늘의 데이터 납품 명세서"
public record KboIngestRequest(
        String baseDate, // "2026-03-29"
        List<FileInfo> files
) {
    public record FileInfo(
            String type, // TEAM_RANK, PLAYER_HITTER, PLAYER_PITCHER, TEAM_STATS, GAME_RESULT
            String s3Path
    ) {}
}