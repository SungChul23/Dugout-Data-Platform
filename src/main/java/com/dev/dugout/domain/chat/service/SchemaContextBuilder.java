package com.dev.dugout.domain.chat.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 역할: 선택된 테이블의 실제 컬럼 설명 구성
@Component
public class SchemaContextBuilder {

    // 실제 DB 컬럼명 기반 스키마 정의
    private static final Map<String, String> TABLE_SCHEMA = Map.ofEntries(

            Map.entry("daily_player_hitter", """
                [daily_player_hitter] 타자 일일 누적 성적 (시즌 누적값)
                PK: base_date(기록날짜), player_id
                JOIN: player_id → player.player_id / team_id → team.id
                핵심컬럼:
                - player_name: 선수명
                - h_avg: 타율 decimal(5,3) - 규정타석 이상만 유효
                - h_hr: 홈런 int
                - h_ops: OPS decimal(5,3)
                - h_obp: 출루율 decimal(5,3)
                - h_slg: 장타율 decimal(5,3)
                - h_rbi: 타점 int
                - h_h: 안타 int
                - h_r: 득점 int
                - h_pa: 타석수 int (규정타석 계산용)
                - h_g: 경기수 int
                - h_so: 삼진 int
                - h_bb: 볼넷 int
                - h_xbh: 장타수 int
                - h_risp: 득점권타율 decimal(5,3)
                - h_gw_rbi: 결승타점 int
                - h_mh: 멀티히트 int
                주의사항:
                - 반드시 MAX(base_date) 기준으로 조회
                - 규정타석 조건: h_pa >= (SELECT MAX(h_g) FROM daily_player_hitter) * 3.1
                - 누적 데이터이므로 특정 기간 비교 시 날짜 간 차이 계산 필요
                """),

            Map.entry("daily_player_pitcher", """
                [daily_player_pitcher] 투수 일일 누적 성적 (시즌 누적값)
                PK: base_date(기록날짜), player_id
                JOIN: player_id → player.player_id / team_id → team.id
                핵심컬럼:
                - player_name: 선수명
                - p_era: 평균자책점 decimal(6,2) - 낮을수록 좋음
                - p_whip: WHIP decimal(5,2) - 낮을수록 좋음
                - p_so: 탈삼진 int
                - p_sv: 세이브 int
                - p_hld: 홀드 int
                - p_w: 승 int / p_l: 패 int
                - p_ip: 소화이닝 decimal(5,2) - 규정이닝 계산용
                - p_g: 경기수 int
                - p_qs: 퀄리티스타트 int
                - p_bsv: 블론세이브 int
                - p_wpct: 승률 decimal(5,3)
                - p_gs: 선발경기 int
                - p_er: 자책점 int
                주의사항:
                - 반드시 MAX(base_date) 기준으로 조회
                - 규정이닝 조건: p_ip >= (SELECT MAX(p_g) FROM daily_player_pitcher) * 1.0
                """),

            Map.entry("daily_team_ranking", """
                [daily_team_ranking] 팀별 순위 및 승패 기록
                PK: ranking_date(기록날짜), team_id  ※ base_date가 아닌 ranking_date 사용!
                JOIN: team_id → team.id
                핵심컬럼:
                - team_rank: 순위 int (1위=1, 숫자 작을수록 상위)
                - wins: 승 int / losses: 패 int / draws: 무 int
                - games_played: 경기수 int
                - win_rate: 승률 decimal(5,3)
                - games_behind: 승차 decimal(4,1)
                - streak: 연승/연패 varchar (예: '3연승', '2연패')
                - last10games: 최근 10경기 결과 varchar
                - home_record: 홈 성적 varchar
                - away_record: 원정 성적 varchar
                주의사항:
                - 반드시 MAX(ranking_date) 기준으로 조회 (base_date 사용 금지!)
                """),

            Map.entry("daily_team_stats", """
                [daily_team_stats] 팀별 타격/투구/수비/주루 통합 성적
                PK: base_date(기록날짜), team_id
                JOIN: team_id → team.id
                타격핵심:
                - avg_h2: 팀타율 / hr_h1: 팀홈런 / ops_h2: 팀OPS
                - obp_h2: 팀출루율 / slg_h2: 팀장타율
                - rbi_h1: 팀타점 / r_h1: 팀득점 / h_h1: 팀안타
                투구핵심:
                - era_p1: 팀ERA / whip_p1: 팀WHIP
                - so_p1: 팀탈삼진 / sv_p1: 팀세이브 / hld_p1: 팀홀드
                - w_p1: 팀승 / qs_p2: 팀QS / bsv_p2: 팀블론세이브
                수비핵심:
                - e_d: 실책 / fpct_d: 수비율 / dp_d: 병살
                주루핵심:
                - sb_r: 도루 / sb_rate_r: 도루성공률
                주의사항:
                - 반드시 MAX(base_date) 기준으로 조회
                """),

            Map.entry("game", """
                [game] 경기 일정 및 결과
                PK: id (auto_increment)
                JOIN: home_team_id → team.id / away_team_id → team.id
                핵심컬럼:
                - game_date: 경기날짜 date
                - game_time: 경기시간 time
                - home_team_id: 홈팀ID / away_team_id: 원정팀ID
                - home_score: 홈팀점수 int (미완료경기는 NULL)
                - away_score: 원정팀점수 int (미완료경기는 NULL)
                - status: 경기상태 varchar
                  SCHEDULED=예정 / FINISHED=완료 / CANCELED=취소
                - stadium_name: 구장명 varchar
                주의사항:
                - 팀명 조회 시 team 테이블과 반드시 JOIN
                - home_team_id, away_team_id 모두 team 테이블과 JOIN 필요
                """),

            Map.entry("prediction_result", """
                [prediction_result] 선수 2026시즌 성적 예측
                PK: predict_id
                JOIN: player_id → player.player_id
                타자 예측컬럼:
                - curr_avg/pred_avg/avg_diff: 현재/예측/차이 타율
                - curr_hr/pred_hr/hr_diff: 현재/예측/차이 홈런
                - curr_ops/pred_ops/ops_diff: 현재/예측/차이 OPS
                - avg_min, avg_max: 타율 예측범위
                - hr_min, hr_max: 홈런 예측범위
                투수 예측컬럼:
                - prob_elite: 엘리트 등극확률 decimal(7,6) (0~1, 1에 가까울수록 높음)
                - role_rank: 보직내 순위 int
                - role_total: 보직내 전체인원 int
                - role_percentile_top: 상위 % decimal
                - era_2025: 2025 실제ERA / fip_2025: FIP / whip_2025: WHIP
                - role: 투수보직 varchar (SP=선발, RP=불펜)
                구분방법: player 테이블의 position_type = '투수' 이면 투수
                """),

            Map.entry("fa_market", """
                [fa_market] FA 예정 선수 등급 및 시장가치 분석
                PK: id
                JOIN: team_id(int) → team.id / pcode → player.kbo_pcode
                핵심컬럼:
                - player_name: 선수명 varchar(50)
                - grade: FA등급 varchar(1) - A/B/C
                - age: 나이 int
                - year: FA자격연도 int (2026 또는 2027)
                - position_type: 포지션유형 (타자/투수)
                - sub_position_type: 세부포지션 varchar
                - stat_offense: 공격점수 decimal(5,2) - 타자용 100점만점
                - stat_defense: 수비점수 decimal(5,2) - 타자용
                - stat_pitching: 구위점수 decimal(5,2) - 투수용
                - stat_stability: 안정성점수 decimal(5,2) - 투수용
                - stat_contribution: 기여도점수 decimal(5,2) - 공통
                - fa_status: FA상태 varchar (잔류/미정/영입/예정)
                - current_salary: 현재연봉 varchar
                - is_fa_target: FA대상여부 bit(1)
                주의사항:
                - FA 대상자만 조회 시 is_fa_target = 1 조건 추가
                - fa_status = '잔류' 인 선수는 반드시 제외
                - 즉 WHERE fa_status != '잔류' 조건 항상 포함
                - 이 데이터는 공식 FA 등급이 아닌 더그아웃이 경기력 기반으로 예측한 등급임
                - 답변 시 반드시 예측값임을 명시하고 FA 시장 등급 분석 메뉴 안내할 것
                """),

            Map.entry("gg_leaderboard", """
                [gg_leaderboard] 골든글러브 수상 예측 (매일 갱신)
                PK: id
                JOIN: player_code → player.kbo_pcode
                핵심컬럼:
                - base_date: 기준날짜 date
                - player_name: 선수명 varchar(50)
                - team_name: 팀명 varchar(50)
                - position: 포지션코드 varchar(10)
                  P=투수 / C=포수 / 1B=1루수 / 2B=2루수 / 3B=3루수
                  SS=유격수 / OF=외야수 / DH=지명타자
                - rank_in_pos: 동일포지션내 순위 int  ※ rank가 아닌 rank_in_pos!
                  예) OF 포지션 중 1위, SS 포지션 중 1위
                - win_prob: 수상확률 double (0~1)
                - win_prob_str: 수상확률 문자열 varchar (예: '45.2%')
                - ai_explanation: AI 분석설명 text
                주의사항:
                - 반드시 MAX(base_date) 기준으로 조회
                - 이 데이터는 실제 골든글러브 결과가 아닌 더그아웃 AI의 예측값임
                - 답변 시 반드시 예측값임을 명시하고 골든글러브 수상 예측 메뉴 안내할 것
                """),

            Map.entry("player", """
                [player] 선수 기본 정보
                PK: player_id
                JOIN: team_id → team.id
                핵심컬럼:
                - player_id: PK bigint
                - name: 선수명 varchar
                - kbo_pcode: KBO공식코드 varchar (UNIQUE)
                - position_type: 포지션 varchar (투수/내야수/외야수/포수)
                - sub_position_type: 세부포지션 varchar
                - back_number: 등번호 int
                - is_predictable: 예측가능여부 bit(1)
                """),

            Map.entry("team", """
                [team] 구단 기본 정보
                PK: id
                핵심컬럼:
                - id: 구단ID bigint
                - name: 구단풀네임 varchar
                - city: 연고지 varchar
                - stadium_name: 홈구장 varchar
                팀ID 매핑: 삼성=1 / 두산=2 / LG=3 / 롯데=4 / KIA=5
                           한화=6 / SSG=7 / 키움=8 / NC=9 / KT=10
                팀명 별칭 매핑 (LIKE 사용 권장):
                  SSG, 랜더스, 인천      → name LIKE '%SSG%'
                  LG, 트윈스            → name LIKE '%LG%'
                  KIA, 기아, 타이거즈   → name LIKE '%KIA%'
                  KT, kt, 수원          → name LIKE '%KT%' OR name LIKE '%wiz%'
                  NC, 다이노스, 창원    → name LIKE '%NC%'
                  삼성, 라이온즈, 대구  → name LIKE '%삼성%'
                  두산, 베어스          → name LIKE '%두산%'
                  롯데, 자이언츠, 부산  → name LIKE '%롯데%'
                  한화, 이글스, 대전    → name LIKE '%한화%'
                  키움, 히어로즈, 넥센  → name LIKE '%키움%'
                """)
    );

    //선택된 테이블 목록으로 스키마 문자열 구성
    public String buildSchema(List<String> selectedTables) {
        return selectedTables.stream()
                .distinct()
                .map(table -> TABLE_SCHEMA.getOrDefault(table, ""))
                .filter(schema -> !schema.isBlank())
                .collect(Collectors.joining("\n"));
    }

    //전체 테이블 목록 (LLM 라우팅용 - 설명만 포함)
    public String buildTableListForRouting() {
        return """
                사용 가능한 테이블 목록:
                1. daily_player_hitter   - 타자 일일 누적 성적 (타율/홈런/타점/OPS 등)
                2. daily_player_pitcher  - 투수 일일 누적 성적 (ERA/WHIP/탈삼진/세이브 등)
                3. daily_team_ranking    - 팀별 순위 및 승패 기록 (순위/승률/승차/연승패)
                4. daily_team_stats      - 팀별 타격/투구/수비/주루 통합 성적
                5. game                  - 경기 일정 및 결과 (날짜/점수/상태/구장)
                6. prediction_result     - 선수 2026시즌 성적 예측값
                7. fa_market             - FA 예정 선수 등급 및 시장가치 분석
                8. gg_leaderboard        - 골든글러브 수상 예측 및 확률
                9. player                - 선수 기본 정보 (이름/포지션/등번호)
                10. team                 - 구단 기본 정보 (이름/연고지/구장)
                """;
    }
}