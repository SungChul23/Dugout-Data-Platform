package com.dev.dugout.global.common;


import org.springframework.stereotype.Component;
import java.util.Map;

//shap 지표 한글화 -> 추후 베드락 리포트에 먹일 예정

@Component
public class MetricTranslator {

    private static final Map<String, String> KOR_METRIC = Map.ofEntries(
            // --- [공통 및 기본 지표] ---
            Map.entry("age", "연령(Age)"),
            Map.entry("G", "출전 경기 수"),
            Map.entry("year_gap", "연차(경력)"),

            // --- [타자 핵심 기술 지표] ---
            Map.entry("AVG", "타율(AVG)"),
            Map.entry("OBP", "출루율(OBP)"),
            Map.entry("SLG", "장타율(SLG)"),
            Map.entry("OPS", "OPS(출루율+장타율)"),
            Map.entry("ISOP", "순수장타력(ISO)"),
            Map.entry("GPA", "GPA(가중 출루율 기반 생산력)"),
            Map.entry("XR", "득점 생산력(XR)"),
            Map.entry("BABIP", "인플레이 타구 안타 비율(BABIP)"),
            Map.entry("Contact_Index", "컨택 지수"),
            Map.entry("MH", "멀티히트(MH) 생산력"),

            // --- [타자 세부 누적/비율 지표] ---
            Map.entry("PA", "타석 수"),
            Map.entry("AB", "타수"),
            Map.entry("H", "안타"),
            Map.entry("2B", "2루타"),
            Map.entry("3B", "3루타"),
            Map.entry("HR", "홈런"),
            Map.entry("TB", "루타 수"),
            Map.entry("RBI", "타점"),
            Map.entry("R", "득점"),
            Map.entry("SB", "도루"),
            Map.entry("BB", "볼넷"),
            Map.entry("HBP", "사구(몸에 맞는 볼)"),
            Map.entry("IBB", "고의사구"),
            Map.entry("SO", "삼진"),
            Map.entry("GDP", "병살타"),
            Map.entry("XBH", "장타 수(2루타 이상)"),

            // --- [타자 고급 분석 지표] ---
            Map.entry("BB_rate", "볼넷률(BB%)"),
            Map.entry("SO_rate", "삼진율(SO%)"),
            Map.entry("BB/K", "볼넷/삼진 비율"),
            Map.entry("P/PA", "타석당 투구 수"),
            Map.entry("GO/AO", "땅볼/뜬공 비율(GO/AO)"),
            Map.entry("GO", "땅볼"),
            Map.entry("AO", "뜬공"),
            Map.entry("RISP", "득점권 타율"),
            Map.entry("GW RBI", "결승 타점"),
            Map.entry("PH-BA", "대타 타율"),
            Map.entry("SAC", "희생번트"),
            Map.entry("SF", "희생플라이"),

            // --- [타자 추세 및 변동성 지표] ---
            Map.entry("AVG_trend", "타율 변화 추세"),
            Map.entry("HR_trend", "홈런 생산 추세"),
            Map.entry("AVG_std_3yr", "최근 3년 타율 변동성"),
            Map.entry("HR_std_3yr", "최근 3년 홈런 변동성"),
            Map.entry("HR_mean_3yr", "최근 3년 홈런 평균"),
            Map.entry("BABIP_std_3yr", "최근 3년 BABIP 변동성"),

            // --- [투수 핵심 기술 지표] ---
            Map.entry("ERA", "평균자책점(ERA)"),
            Map.entry("FIP", "수비 무관 평균자책점(FIP)"),
            Map.entry("WHIP", "WHIP(이닝당 출루 허용률)"),
            Map.entry("WPCT", "승률"),
            Map.entry("QS", "퀄리티 스타트(QS)"),
            Map.entry("LOB%", "잔루 처리율(LOB%)"),

            // --- [투수 세부 누적/비율 지표] ---
            Map.entry("IP", "이닝 수"),
            Map.entry("W", "승리"),
            Map.entry("L", "패전"),
            Map.entry("HLD", "홀드"),
            Map.entry("SVO", "세이브 기회"),
            Map.entry("BSV", "블론세이브"),
            Map.entry("GF", "경기 종료 투수(GF)"),
            Map.entry("ER", "자책점"),
            Map.entry("TS", "총 투구 수"),
            Map.entry("BK", "보크"),
            Map.entry("WP", "폭투"),
            Map.entry("Wgs", "선발승"),
            Map.entry("Wgr", "구원승"),

            // --- [투수 고급 분석 및 추세 지표] ---
            Map.entry("K/9", "9이닝당 탈삼진"),
            Map.entry("BB/9", "9이닝당 볼넷"),
            Map.entry("HR/9", "9이닝당 홈런 허용"),
            Map.entry("K/BB", "탈삼진/볼넷 비율"),
            Map.entry("ERA_trend", "평균자책점 변화 추세"),
            Map.entry("FIP_diff", "FIP 대비 성적 차이"),
            Map.entry("FIP_C", "FIP 상수 값")
    );

    public String translate(String feature) {
        return KOR_METRIC.getOrDefault(feature, feature);
    }
}
