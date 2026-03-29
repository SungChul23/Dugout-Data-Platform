package com.dev.dugout.infrastructure.aws.dto;

import java.util.List;

public record KboIngestRequest(
        String baseDate, // "2026-03-29"
        List<FileInfo> files
) {
    public record FileInfo(
            String type, // TEAM_RANK, PLAYER_HITTER, PLAYER_PITCHER, TEAM_STATS
            String s3Path
    ) {}
}