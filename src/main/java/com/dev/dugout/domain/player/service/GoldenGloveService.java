package com.dev.dugout.domain.player.service;


import com.dev.dugout.domain.player.dto.GoldenGloveResponseDto;
import com.dev.dugout.domain.player.entity.GoldenGlovePrediction;
import com.dev.dugout.domain.player.repository.GoldenGlovePredictionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoldenGloveService {

    private final GoldenGlovePredictionRepository repository;

    @Transactional(readOnly = true)
    public GoldenGloveResponseDto.LeaderboardResponse getLatestLeaderboard() {

        LocalDate latestDate = repository.findLatestBaseDate()
                .orElseThrow(() -> new RuntimeException("예측된 골든글러브 데이터가 존재하지 않습니다."));

        // Object 배열 리스트로 받아옴
        List<Object[]> results = repository.findTopContendersWithSubPosition(latestDate);

        // Entity와 서브 포지션 문자열을 조합하여 DTO로 변환 후 그룹화
        Map<String, List<GoldenGloveResponseDto.PlayerPredictionDto>> groupedLeaderboard = results.stream()
                .map(result -> {
                    GoldenGlovePrediction entity = (GoldenGlovePrediction) result[0];
                    String subPosition = (String) result[1];
                    return convertToDto(entity, subPosition);
                })
                .collect(Collectors.groupingBy(GoldenGloveResponseDto.PlayerPredictionDto::getPosition));

        return GoldenGloveResponseDto.LeaderboardResponse.builder()
                .baseDate(latestDate.toString())
                .leaderboardByPosition(groupedLeaderboard)
                .build();
    }

    // DTO 변환 메서드에 subPosition 파라미터 추가
    private GoldenGloveResponseDto.PlayerPredictionDto convertToDto(GoldenGlovePrediction entity, String subPosition) {

        // 만약 Player 테이블에 값이 없거나 Null일 경우를 대비한 기본값 방어 로직
        String displayPosition = (subPosition != null && !subPosition.isBlank()) ? subPosition : entity.getPosition();

        return GoldenGloveResponseDto.PlayerPredictionDto.builder()
                .playerCode(entity.getPlayerCode())
                .playerName(entity.getPlayerName())
                .teamName(entity.getTeamName())
                .position(entity.getPosition())     // "OF" (프론트에서 탭 구분할 때 사용)
                .subPosition(displayPosition)       // "우익수" (프론트에서 유저에게 보여줄 때 사용)
                .winProbStr(entity.getWinProbStr())
                .rank(entity.getRank())
                .top3Positive(entity.getTop3Positive())
                .top1Negative(entity.getTop1Negative())
                .aiExplanation(entity.getAiExplanation())
                .build();
    }
}
