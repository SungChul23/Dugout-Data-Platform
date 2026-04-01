package com.dev.dugout.infrastructure.aws.strategy;

import com.dev.dugout.domain.team.entity.Game;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.GameRepository;
import com.dev.dugout.domain.team.repository.TeamRepository;
import com.dev.dugout.global.common.S3JsonReader;
import com.dev.dugout.infrastructure.aws.batch.KboDataCategory;
import com.dev.dugout.infrastructure.aws.batch.KboIngestStrategy;
import com.dev.dugout.infrastructure.aws.dto.GameResultIngestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameResultIngestStrategy implements KboIngestStrategy {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final S3JsonReader s3JsonReader;

    @Override
    public KboDataCategory getCategory() {
        return KboDataCategory.GAME_RESULT;
    }

    // [최적화 방어 로직]
    // 해당 날짜의 경기들이 모두 끝났는지(FINISHED or CANCELED) 확인.
    // 단 하나라도 SCHEDULED나 LIVE 상태가 있다면 갱신이 필요하므로 false를 리턴

    @Override
    public boolean isAlreadyIngested(LocalDate baseDate) {
        List<Game> dailyGames = gameRepository.findAllByGameDate(baseDate);

        // 경기가 아예 없다면 S3에서 읽어와서 Insert를 시도해야 하므로 false 리턴
        if (dailyGames.isEmpty()) {
            return false;
        }

        // 모든 경기가 '종료' 또는 '취소' 상태인지 검사
        boolean isAllCompleted = dailyGames.stream()
                .allMatch(game -> "FINISHED".equals(game.getStatus()) || "CANCELED".equals(game.getStatus()));

        if (isAllCompleted) {
            log.info(">>>> [GAME_RESULT] {} 의 모든 경기가 이미 종료(FINISHED/CANCELED) 상태입니다. S3 조회 및 입고를 건너뜁니다.", baseDate);
        }

        return isAllCompleted;
    }

    @Override
    @Transactional
    public void ingest(String s3Path, LocalDate baseDate) {

        // 1. S3에서 JSON 데이터 읽기
        List<GameResultIngestDto> dtos = s3JsonReader.read(s3Path, GameResultIngestDto.class);
        if (dtos.isEmpty()) return;

        int updatedCount = 0;
        int insertedCount = 0;

        log.info(">>>> [GAME_RESULT] 경기 결과 업데이트 시작 (총 {}건)", dtos.size());

        for (GameResultIngestDto dto : dtos) {
            LocalDate gameDate = LocalDate.parse(dto.getGameDate());

            // 2. 기존 경기 조회 (날짜, 홈팀, 원정팀 복합 키 역할)
            Optional<Game> optionalGame = gameRepository.findByGameDateAndHomeTeamIdAndAwayTeamId(
                    gameDate, dto.getHomeTeamId(), dto.getAwayTeamId()
            );

            if (optionalGame.isPresent()) {
                // [Update] 이미 일정이 존재하는 경우 -> 더티 체킹을 통한 상태 및 점수 업데이트
                Game game = optionalGame.get();
                game.updateResult(dto.getHomeScore(), dto.getAwayScore(), dto.getStatus());
                updatedCount++;
            } else {
                // [Insert] 우천 취소 후 재편성 등 DB에 아예 없던 새로운 경기일 경우
                Team homeTeam = teamRepository.findById(dto.getHomeTeamId()).orElse(null);
                Team awayTeam = teamRepository.findById(dto.getAwayTeamId()).orElse(null);

                if (homeTeam != null && awayTeam != null) {
                    Game newGame = Game.builder()
                            .gameDate(gameDate)
                            // db의 game_time 타입(LocalTime, String 등)에 맞춰 파싱 처리 필요
                            // .gameTime(LocalTime.parse(dto.getGameTime()))
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .homeScore(dto.getHomeScore())
                            .awayScore(dto.getAwayScore())
                            .status(dto.getStatus())
                            // stadium 등 부가 정보 매핑 필요시 추가
                            .build();

                    gameRepository.save(newGame);
                    insertedCount++;
                } else {
                    log.warn(">>>> [GAME_RESULT] 팀 정보를 찾을 수 없어 새로운 경기 일정을 추가할 수 없습니다. Home ID: {}, Away ID: {}",
                            dto.getHomeTeamId(), dto.getAwayTeamId());
                }
            }
        }

        log.info(">>>> [GAME_RESULT 입고 완료] 업데이트: {}건, 신규추가: {}건", updatedCount, insertedCount);
    }
}