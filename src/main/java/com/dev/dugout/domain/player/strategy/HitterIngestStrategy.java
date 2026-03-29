package com.dev.dugout.domain.player.strategy;

import com.dev.dugout.domain.player.dto.HitterIngestDto;
import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.DailyPlayerHitterRepository;
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
public class HitterIngestStrategy implements KboIngestStrategy {

    private final DailyPlayerHitterRepository hitterRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;

    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.PLAYER_HITTER;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {
        // 1. JSON 읽기
        List<HitterIngestDto> dtos = s3JsonReader.read(s3Path, HitterIngestDto.class);

        // 2. 엔티티 변환 (pcode로 Player 찾기 포함)
        List<DailyPlayerHitter> entities = dtos.stream().map(dto -> {
            Player player = playerRepository.findByKboPcode(dto.getPcode())
                    .orElseThrow(() -> new EntityNotFoundException("선수 없음: " + dto.getPcode()));

            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new EntityNotFoundException("팀 없음: " + dto.getTeamId()));

            return dto.toEntity(baseDate, player, team);
        }).toList();

        // 3. 저장
        hitterRepository.saveAll(entities);
    }
}