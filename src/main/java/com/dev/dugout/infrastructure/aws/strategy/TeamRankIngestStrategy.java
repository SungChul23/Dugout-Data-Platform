package com.dev.dugout.infrastructure.aws.strategy;

import com.dev.dugout.domain.team.dto.TeamRankIngestDto;
import com.dev.dugout.domain.team.entity.DailyTeamRanking;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.DailyTeamRankingRepository;
import com.dev.dugout.domain.team.repository.TeamRepository;
import com.dev.dugout.infrastructure.aws.batch.KboDataCategory;
import com.dev.dugout.infrastructure.aws.batch.KboIngestStrategy;
import com.dev.dugout.global.common.S3JsonReader;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRankIngestStrategy implements KboIngestStrategy {

    private final DailyTeamRankingRepository rankingRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;


    @Override
    public boolean isAlreadyIngested(LocalDate baseDate) {
        return rankingRepository.existsByBaseDate(baseDate);
    }
    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.TEAM_RANK;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {
        List<TeamRankIngestDto> dtos = s3JsonReader.read(s3Path, TeamRankIngestDto.class);

        List<DailyTeamRanking> entities = dtos.stream().map(dto -> {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new EntityNotFoundException("팀 없음: " + dto.getTeamId()));

            return dto.toEntity(baseDate, team);
        }).toList();

        rankingRepository.saveAll(entities);
    }
}