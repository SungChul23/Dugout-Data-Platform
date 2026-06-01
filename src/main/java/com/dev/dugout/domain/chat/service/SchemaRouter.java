package com.dev.dugout.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
// 역할: 1차 하드코딩 테이블 선택
public class SchemaRouter {

    // 키워드 → 필요 테이블 매핑
    private static final List<Map.Entry<List<String>, List<String>>> KEYWORD_TABLE_MAP = List.of(

            // 타자 성적
            Map.entry(
                    List.of("타율", "홈런", "타점", "안타", "OPS", "ops", "출루율", "장타율",
                            "타자", "타격", "득점", "삼진", "볼넷", "도루", "장타", "타석",
                            "멀티히트", "결승타", "득점권", "RISP", "risp"),
                    List.of("daily_player_hitter", "player", "team")
            ),

            // 투수 성적
            Map.entry(
                    List.of("ERA", "era", "방어율", "탈삼진", "투수", "세이브", "홀드",
                            "WHIP", "whip", "이닝", "승", "블론", "퀄리티", "QS", "qs",
                            "선발", "불펜", "구원"),
                    List.of("daily_player_pitcher", "player", "team")
            ),

            // 팀 순위
            Map.entry(
                    List.of("순위", "승률", "승차", "연승", "연패", "최근경기",
                            "팀순위", "몇위", "위야", "위임"),
                    List.of("daily_team_ranking", "team")
            ),

            // 팀 성적 (팀 단위 지표)
            Map.entry(
                    List.of("팀타율", "팀홈런", "팀ERA", "팀성적", "구단성적",
                            "팀방어율", "팀타격", "팀투구"),
                    List.of("daily_team_stats", "team")
            ),

            // 경기 일정/결과
            Map.entry(
                    List.of("경기", "일정", "결과", "스코어", "오늘경기", "내일경기",
                            "어제경기", "이겼", "졌", "취소", "비겼", "vs", "대전",
                            "구장", "홈경기", "원정"),
                    List.of("game", "team")
            ),

            // 선수 미래 예측
            Map.entry(
                    List.of("예측", "내년", "전망", "시즌후", "예상성적", "엘리트",
                            "확률", "2026예측", "미래"),
                    List.of("prediction_result", "player", "team")
            ),

            // FA 분석
            Map.entry(
                    List.of("FA", "fa", "자유계약", "연봉", "FA등급", "이적",
                            "잔류", "영입", "FA시장"),
                    List.of("fa_market", "team")
            ),

            // 골든글러브
            Map.entry(
                    List.of("골든글러브", "골글", "수상", "골든장갑"),
                    List.of("gg_leaderboard", "player")
            ),

            Map.entry(
                    List.of("성적"),
                    List.of("daily_player_hitter", "daily_player_pitcher", "player", "team")
            )
    );

    /**
     * 질문에서 키워드를 감지하여 관련 테이블 목록 반환
     * 매칭 실패 시 빈 리스트 반환 → LlmSchemaRouter로 위임
     */
    public List<String> route(String question) {
        Set<String> matchedTables = new LinkedHashSet<>();

        for (Map.Entry<List<String>, List<String>> entry : KEYWORD_TABLE_MAP) {
            List<String> keywords = entry.getKey();
            List<String> tables = entry.getValue();

            boolean matched = keywords.stream()
                    .anyMatch(keyword -> question.contains(keyword));

            if (matched) {
                matchedTables.addAll(tables);
            }
        }

        // player, team은 거의 항상 필요하므로 기본 추가
        matchedTables.add("player");
        matchedTables.add("team");

        if (matchedTables.size() > 2) { // player + team 외 다른 테이블이 있으면
            log.info("[SchemaRouter] 하드코딩 매칭 성공: {}", matchedTables);
            return new ArrayList<>(matchedTables);
        }

        log.info("[SchemaRouter] 하드코딩 매칭 실패 → LLM 라우팅으로 위임");
        return Collections.emptyList();
    }
}