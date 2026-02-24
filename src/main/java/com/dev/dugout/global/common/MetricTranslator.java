package com.dev.dugout.global.common;

import org.springframework.stereotype.Component;
import java.util.Map;

// SHAP 분석 지표(Feature)를 AI 리포트용 'English(한글)' 형식으로 변환하는 컴포넌트
@Component
public class MetricTranslator {

    private static final Map<String, String> KOR_METRIC = Map.ofEntries(
            // --- [공통 및 기본 지표] ---
            Map.entry("age", "Age(연령)"),
            Map.entry("G", "G(출전 경기 수)"),
            Map.entry("year_gap", "연차(경력)"),

            // --- [타자 핵심 기술 지표] ---
            Map.entry("AVG", "AVG(타율)"),
            Map.entry("OBP", "OBP(출루율)"),
            Map.entry("SLG", "SLG(장타율)"),
            Map.entry("OPS", "OPS(출루율+장타율)"),
            Map.entry("ISOP", "ISO(순수장타력)"),
            Map.entry("GPA", "GPA(가중 출루율 기반 생산력)"),
            Map.entry("XR", "XR(득점 생산력)"),
            Map.entry("BABIP", "BABIP(인플레이 타구 안타 비율)"),
            Map.entry("Contact_Index", "컨택 지수(Contact Index)"),
            Map.entry("MH", "MH(멀티히트 생산력)"),

            // --- [타자 누적 및 세부 지표] ---
            Map.entry("PA", "PA(타석 수)"),
            Map.entry("AB", "AB(타수)"),
            Map.entry("H", "H(안타)"),
            Map.entry("2B", "2B(2루타)"),
            Map.entry("3B", "3B(3루타)"),
            Map.entry("HR", "HR(홈런)"),
            Map.entry("RBI", "RBI(타점)"),
            Map.entry("R", "R(득점)"),
            Map.entry("BB", "BB(볼넷)"),
            Map.entry("HBP", "HBP(사구)"),
            Map.entry("IBB", "IBB(고의사구)"),
            Map.entry("SO", "SO(삼진)"),
            Map.entry("GDP", "GDP(병살타)"),
            Map.entry("XBH", "XBH(장타 수)"),
            Map.entry("TB", "TB(총 루타)"),

            // --- [타자 고급 분석 및 비율 지표] ---
            Map.entry("BB_rate", "BB%(볼넷률)"),
            Map.entry("SO_rate", "SO%(삼진율)"),
            Map.entry("BB/K", "BB/K(볼넷/삼진 비율)"),
            Map.entry("P/PA", "P/PA(타석당 투구 수)"),
            Map.entry("GO/AO", "GO/AO(땅볼/뜬공 비율)"),
            Map.entry("GO", "GO(땅볼)"),
            Map.entry("AO", "AO(뜬공)"),
            Map.entry("RISP", "RISP(득점권 타율)"),
            Map.entry("GW RBI", "GW RBI(결승 타점)"),
            Map.entry("PH-BA", "PH-BA(대타 타율)"),
            Map.entry("SAC", "SAC(희생번트)"),
            Map.entry("SF", "SF(희생플라이)"),

            // --- [타자 추세 및 변동성 지표] ---
            Map.entry("AVG_trend", "AVG 추세(타율 변화 흐름)"),
            Map.entry("HR_trend", "HR 추세(홈런 생산 흐름)"),
            Map.entry("AVG_std_3yr", "AVG 변동성(최근 3년 타율 기복)"),
            Map.entry("HR_std_3yr", "HR 변동성(최근 3년 홈런 기복)"),
            Map.entry("HR_mean_3yr", "HR 평균(최근 3년 홈런 평균)"),
            Map.entry("BABIP_std_3yr", "BABIP 변동성(최근 3년 타구 운 기복)"),

            // --- [투수 핵심 기술 지표] ---
            Map.entry("ERA", "ERA(평균자책점)"),
            Map.entry("FIP", "FIP(수비 무관 평균자책점)"),
            Map.entry("WHIP", "WHIP(이닝당 출루 허용률)"),
            Map.entry("WPCT", "WPCT(승률)"),
            Map.entry("QS", "QS(퀄리티 스타트)"),
            Map.entry("LOB%", "LOB%(잔루 처리율)"),

            // --- [투수 누적 및 세부 지표] ---
            Map.entry("IP", "IP(이닝 수)"),
            Map.entry("W", "W(승리)"),
            Map.entry("L", "L(패전)"),
            Map.entry("HLD", "HLD(홀드)"),
            Map.entry("SVO", "SVO(세이브 기회)"),
            Map.entry("BSV", "BSV(블론세이브)"),
            Map.entry("GF", "GF(경기 종료 투수)"),
            Map.entry("ER", "ER(자책점)"),
            Map.entry("TS", "TS(총 투구 수)"),
            Map.entry("BK", "BK(보크)"),
            Map.entry("WP", "WP(폭투)"),
            Map.entry("Wgs", "Wgs(선발승)"),
            Map.entry("Wgr", "Wgr(구원승)"),

            // --- [투수 고급 분석 및 추세 지표] ---
            Map.entry("K/9", "K/9(9이닝당 탈삼진)"),
            Map.entry("BB/9", "BB/9(9이닝당 볼넷)"),
            Map.entry("HR/9", "HR/9(9이닝당 홈런 허용)"),
            Map.entry("K/BB", "K/BB(탈삼진/볼넷 비율)"),
            Map.entry("ERA_trend", "ERA 추세(평균자책점 변화 흐름)"),
            Map.entry("FIP_diff", "FIP_diff(성적-기대치 차이)"),
            Map.entry("FIP_C", "FIP 상수")
    );


    public String translate(String feature) {
        return KOR_METRIC.getOrDefault(feature, feature);
    }
}