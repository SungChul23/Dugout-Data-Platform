package com.dev.dugout.domain.player.strategy;

import com.dev.dugout.domain.player.dto.PitcherIngestDto;
import com.dev.dugout.domain.player.entity.DailyPlayerPitcher;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.DailyPlayerPitcherRepository;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.entity.Team;
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
public class PitcherIngestStrategy implements KboIngestStrategy {

    private final DailyPlayerPitcherRepository pitcherRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;

    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.PLAYER_PITCHER;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {
        List<PitcherIngestDto> dtos = s3JsonReader.read(s3Path, PitcherIngestDto.class);

        List<DailyPlayerPitcher> entities = dtos.stream().map(dto -> {
            Player player = playerRepository.findByKboPcode(dto.getPcode())
                    .orElseThrow(() -> new EntityNotFoundException("선수 없음: " + dto.getPcode()));

            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new EntityNotFoundException("팀 없음: " + dto.getTeamId()));

            return dto.toEntity(baseDate, player, team);
        }).toList();

        pitcherRepository.saveAll(entities);
    }
}