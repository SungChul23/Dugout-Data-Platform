package com.dev.dugout.infrastructure.aws.strategy;

import com.dev.dugout.infrastructure.aws.dto.HitterIngestDto;
import com.dev.dugout.domain.player.entity.DailyPlayerHitter;
import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.DailyPlayerHitterRepository;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.TeamRepository;
import com.dev.dugout.infrastructure.aws.batch.KboDataCategory;
import com.dev.dugout.infrastructure.aws.batch.KboIngestStrategy;
import com.dev.dugout.global.common.S3JsonReader;
import com.dev.dugout.infrastructure.aws.service.PlayerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HitterIngestStrategy implements KboIngestStrategy {

    private final DailyPlayerHitterRepository hitterRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;
    private final PlayerSyncService playerSyncService;


    @Override
    public boolean isAlreadyIngested(LocalDate baseDate) {
        // 해당 날짜의 데이터가 하나라도 있으면 이미 적재된 것으로 판단
        return hitterRepository.existsByBaseDate(baseDate);
    }
    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.PLAYER_HITTER;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {

        // 1. JSON 읽기
        List<HitterIngestDto> dtos = s3JsonReader.read(s3Path, HitterIngestDto.class);
        if (dtos.isEmpty()) return;

        //  2. [선제 방어] 누락된 선수들 먼저 찾아서 람다로 동기화
        Set<String> allPcodes = new HashSet<>();
        for (HitterIngestDto dto : dtos) {
            allPcodes.add(dto.getPcode());
        }

        // DB에 존재하지 않는 PCODE만 필터링
        Set<String> missingPcodes = new HashSet<>();
        for (String pcode : allPcodes) {
            if (!playerRepository.existsByKboPcode(pcode)) {
                missingPcodes.add(pcode);
            }
        }

        // 누락된 선수가 있다면 람다 호출하여 DB 채우기
        if (!missingPcodes.isEmpty()) {
            log.info(">>>> 🛠️ 누락된 선수 {}명 발견! 동기화 서비스를 가동합니다.", missingPcodes.size());
            playerSyncService.syncMissingPlayers(missingPcodes);
        }

        // ---------------------------------------------------------

        //  결과 수집용 바구니들
        List<DailyPlayerHitter> entitiesToSave = new ArrayList<>();
        int totalCount = dtos.size();

        log.info(">>>> [PLAYER_HITTER] 데이터 분석 및 엔티티 변환 시작");
        log.info(">>>> [PLAYER_HITTER] 데이터 분석 시작 (총 {}건)", totalCount);

        // 루프를 돌며 선수 존재 여부 확인 (stream 대신 for loop로 안정성 확보)
        for (HitterIngestDto dto : dtos) {
            String pcode = dto.getPcode();

            // 선수와 팀을 동시에 찾음
            Optional<Player> playerOpt = playerRepository.findByKboPcode(pcode);
            Optional<Team> teamOpt = teamRepository.findById(dto.getTeamId());

            if (playerOpt.isPresent() && teamOpt.isPresent()) {
                // 둘 다 있을 때만 저장 목록에 추가
                entitiesToSave.add(dto.toEntity(baseDate, playerOpt.get(), teamOpt.get()));
            } else {
                // 선수가 없으면 누락 리스트에 PCODE 추가
                missingPcodes.add(pcode);
            }
        }

        // 필터링된 엔티티들만 벌크 저장
        if (!entitiesToSave.isEmpty()) {
            hitterRepository.saveAll(entitiesToSave);
        }

        //  [최종 누락 리포트 출력]
        printHitterMissingReport(totalCount, entitiesToSave.size(), missingPcodes);
    }

    private void printHitterMissingReport(int total, int success, Set<String> missingPcodes) {
        if (missingPcodes.isEmpty()) {
            log.info(">>>> [타자 입고 성공] 모든 타자가 매칭되었습니다. (총 {}건)", success);
        } else {
            log.error(">>>> [타자 누락 리포트] DB에 없는 타자 {}명 발견!", missingPcodes.size());
            log.error(">>>> 누락된 타자 PCODE 리스트: {}", missingPcodes);
            log.warn(">>>>  요약: 성공 {}건 / 누락 {}건 (전체 {}건)", success, missingPcodes.size(), total);
        }
    }
}