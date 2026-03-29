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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j // 로그 기록을 위해 추가
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

        // ✨ 결과 수집용 바구니들
        List<DailyPlayerPitcher> entitiesToSave = new ArrayList<>();
        Set<String> missingPcodes = new TreeSet<>(); // 정렬된 누락 리스트
        int totalCount = dtos.size();

        log.info(">>>> [PLAYER_PITCHER] 데이터 입고 분석 시작 (총 {}건)", totalCount);

        for (PitcherIngestDto dto : dtos) {
            String pcode = dto.getPcode();

            // 1. 선수 존재 확인
            Optional<Player> playerOpt = playerRepository.findByKboPcode(pcode);
            // 2. 팀 존재 확인 (팀은 웬만하면 다 있겠지만 방어적으로 체크)
            Optional<Team> teamOpt = teamRepository.findById(dto.getTeamId());

            if (playerOpt.isPresent() && teamOpt.isPresent()) {
                // 둘 다 있으면 저장 목록에 추가
                entitiesToSave.add(dto.toEntity(baseDate, playerOpt.get(), teamOpt.get()));
            } else {
                // 하나라도 없으면 누락 리스트에 추가 (PCODE 기준)
                missingPcodes.add(pcode);
            }
        }

        // 3. 존재하는 선수들 데이터만 벌크 저장
        if (!entitiesToSave.isEmpty()) {
            pitcherRepository.saveAll(entitiesToSave);
        }

        // 📊 [누락 리포트 출력]
        printMissingReport(totalCount, entitiesToSave.size(), missingPcodes);
    }

    private void printMissingReport(int total, int success, Set<String> missingPcodes) {
        if (missingPcodes.isEmpty()) {
            log.info(">>>>  [투수 입고 완료] 모든 선수가 매칭되었습니다. (총 {}건)", success);
        } else {
            log.error(">>>> [투수 누락 리포트] DB에 없는 투수 {}명 발견!", missingPcodes.size());
            log.error(">>>> 누락된 투수 PCODE 리스트: {}", missingPcodes);
            log.warn(">>>> 성공: {}건 / 실패: {}건", success, missingPcodes.size());
        }
    }
}