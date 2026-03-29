package com.dev.dugout.domain.team.strategy;


import com.dev.dugout.domain.team.dto.TeamRankIngestDto;
import com.dev.dugout.domain.team.dto.TeamStatsIngestDto;
import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.DailyTeamStats;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.repository.DailyTeamStatsRepository;
import com.dev.dugout.domain.team.repository.TeamRepository;
import com.dev.dugout.global.batch.KboDataCategory;
import com.dev.dugout.global.batch.KboIngestStrategy;
import com.dev.dugout.global.common.S3JsonReader;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamStatsIngestStrategy implements KboIngestStrategy {

    private final DailyTeamStatsRepository teamStatsRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;

    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.TEAM_STATS;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {
        List<TeamStatsIngestDto> dtos = s3JsonReader.read(s3Path, TeamStatsIngestDto.class);

        List<DailyTeamStats> entities = dtos.stream().map(dto -> {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new EntityNotFoundException("팀 없음: " + dto.getTeamId()));

            return dto.toEntity(baseDate, team);
        }).toList();

        teamStatsRepository.saveAll(entities);
    }
}