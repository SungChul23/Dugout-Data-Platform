package com.dev.dugout.domain.player.service;


import com.dev.dugout.global.common.MetricTranslator;
import com.dev.dugout.global.common.S3CacheManager;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockClientFacade;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockErrorStrategy;
import com.dev.dugout.infrastructure.aws.bedrock.BedrockMessage;
import com.dev.dugout.infrastructure.aws.s3.S3Service;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportBedrockService {

    private final S3Service s3Service;
    private final BedrockClientFacade bedrockClientFacade;
    private final S3CacheManager s3CacheManager;
    private final MetricTranslator metricTranslator;

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String CALLER_NAME = "ReportBedrockService";
    private static final String FALLBACK_MESSAGE = "AI 리포트 생성 중 오류가 발생했습니다.";
    private static final String CACHE_KEY = "report-master";

    private static final String SYSTEM_PROMPT = "너는 KBO 데이터 전략가야. 아래 규칙을 어기면 리포트가 파괴되니 반드시 지켜:\n" +
            "1. 모든 강조(`**`)는 단어에 '밀착'시킨다. (예: `**타율**` (O), `** 타율 **` (X))\n" +
            "2. 문장 끝에 마침표(.)가 있다면 반드시 마침표 '뒤'가 아닌 '앞'에서 `**`를 닫는다. (예: `...합니다.**` (O))\n" +
            "3. 지표 이름(AVG, HR 등)이 나오면 무조건 한글명을 포함해 `**지표명(한글명)**`으로 통일한다.\n" +
            "4. 내용이 끝난 뒤 의미 없는 `**` 찌꺼기를 절대 남기지 마.";

    //서버 시작 시 S3 마스터 파일을 읽어 메모리에 캐싱
    @PostConstruct
    public void init() {
        log.info("====> [초기화] S3 마스터 데이터를 메모리에 로드합니다.(선수 성적 예측)");
        s3CacheManager.load(CACHE_KEY, s3Service.fetchMasterJson(),
                obj -> extractPcode(obj), "타자");
        s3CacheManager.load(CACHE_KEY, s3Service.fetchPitcherMasterJson(),
                obj -> extractPcode(obj), "투수");
    }

    private String extractPcode(JSONObject obj) {
        if (obj.has("kbo_pcode")) {
            return String.valueOf(obj.get("kbo_pcode"));
        } else if (obj.has("pcode")) {
            return String.valueOf(obj.get("pcode"));
        }
        return null;
    }

    //포지션에 따라 타자 OR 투수 프롬프트를 분기 생성
    public String generatePlayerReport(PredictionResult pred) {
        String pcode = pred.getPlayer().getKboPcode();
        String s3Context = s3CacheManager.getOrDefault(CACHE_KEY, pcode, "기본 선수 정보만 제공됨");

        if (s3Context == null) {
            log.warn("#### [데이터 누락] PCODE: {} 에 해당하는 마스터 데이터를 찾을 수 없습니다.", pcode);
            return "해당 선수의 세부 분석 데이터가 준비되지 않아 리포트를 생성할 수 없습니다.";
        }

        String prompt;
        if ("투수".equals(pred.getPlayer().getPositionType())) {
            prompt = constructPitcherPrompt(pred, s3Context);
        } else {
            prompt = constructHitterPrompt(pred, s3Context);
        }

        return bedrockClientFacade.invoke(
                MODEL_ID, 2000, 0.3,
                SYSTEM_PROMPT,
                List.of(BedrockMessage.user(prompt)),
                BedrockErrorStrategy.RETURN_FALLBACK,
                FALLBACK_MESSAGE,
                CALLER_NAME
        );
    }

    //[타자용 프롬프트 생성]
    private String constructHitterPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        String shapSummary = extractHitterShap(s3Context);

        String perf2025 = "데이터 확인 중";
        try {
            JSONObject json = new JSONObject(s3Context);
            JSONObject p2025 = json.getJSONObject("performance_2025");
            perf2025 = String.format("AVG(타율) %.3f, HR(홈런) %d개, OPS %.3f",
                    p2025.getDouble("avg"), p2025.getInt("hr"), p2025.getDouble("ops"));
        } catch (Exception ignored) {}

        return String.format(
                "너는 KBO 리그 데이터 전략가이자 수석 스카우터입니다. 아래 제공된 [성적 결정 핵심 지표]를 분석의 '유일한 정답지'로 간주하여 %s 선수의 리포트를 작성하십시오.\n\n" +

                        "[분석 기초 데이터]\n" +
                        "- 선수: %s\n" +
                        "- 2025년 실제 성적: %s\n" +
                        "- 2026년 예측치:\n" +
                        "  * AVG(타율): %.3f (범위: %.3f ~ %.3f)\n" +
                        "  * HR(홈런): %d개 (범위: %d ~ %d)\n" +
                        "  * OPS: %.3f (범위: %.3f ~ %.3f)\n" +
                        "- 성적 결정 핵심 지표 (분석의 핵심 근거): \n%s\n\n" +

                        "[리포트 작성 지침 - 데이터 활용 필수]\n" +
                        "1. **지표 사용 강제**: '성적 결정 핵심 지표'에 나열된 모든 한글 명칭(예: **MH(멀티히트 생산력)** 등)을 본문 서술의 주어로 사용하십시오. 데이터가 없어서 분석이 어렵다는 식의 회피 문장은 절대 금지입니다.\n" +
                        "2. **강조 표시 규칙**: \n" +
                        "   - 본문에 지표 이름이 등장할 때마다 반드시 **지표명** 형식을 지키십시오. (예: **BABIP(인플레이 타구 안타 비율)**)\n" +
                        "   - 각 섹션의 마지막에는 해당 문단의 핵심 결론 문장 하나를 통째로 `**`로 감싸 강조하십시오.\n" +
                        "3. **지표 제한**: 제공된 리스트에 없는 지표는 상상해서 쓰지 마십시오. '정신력', '노력' 같은 주관적 단어도 배제하십시오.\n" +
                        "4. **2025년 성적 브릿지**: ## 요약문 바로 아래, 2025년 성적(%s)이 2026년 예측 결과에 기술적으로 어떤 개연성을 주는지 연결하여 서술하십시오.\n" +
                        "5. **소제목 완성**: ### 소제목의 따옴표(\"\") 안에는 가이드 텍스트가 아닌, 지표가 시사하는 '기술적 결론'을 직접 창작해 넣으십시오.\n" +
                        "6. **문단 구성**:\n" +
                        "   - [최상단]: ## [2026년 기술적 위상 요약 한 줄]\n" +
                        "   - [브릿지]: (2025년 성적 리뷰 및 2026년과의 기술적 연결 고리 서술)\n" +
                        "   - ### 1. 2026 시즌 전체 전망: \"(데이터 기반의 성적 추세 요약)\"\n" +
                        "   - ### 2. 성장을 이끄는 기술적 동력: \"(제공된 상승 기여 지표들의 시너지 요약)\"\n" +
                        "   - ### 3. 주의해야 할 변수와 리스크: \"(제공된 하락 기여 지표들의 잠재적 리스크 요약)\"\n" +
                        "   - ### 4. 최종 기술 종합 평론: (지표들을 총망라하여 2026년의 전술적 가치를 상세히 기술. 인사말 금지)\n\n" +
                        "- 말투: 냉철한 전문가용 경어체(~합니다)를 사용하십시오. 이모지는 금지입니다.",
                playerName, playerName, perf2025,
                pred.getPredAvg(), pred.getAvgMin(), pred.getAvgMax(),
                pred.getPredHr(), pred.getHrMin(), pred.getHrMax(),
                pred.getPredOps(), pred.getOpsMin(), pred.getOpsMax(),
                shapSummary, perf2025
        );
    }

    // [투수용 프롬프트 생성]
    private String constructPitcherPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        String shapSummary = extractPitcherShap(s3Context);

        String perf2025 = "데이터 확인 중";
        int age = 0;
        try {
            JSONObject json = new JSONObject(s3Context);
            JSONObject p2025 = json.getJSONObject("performance_2025");
            perf2025 = String.format("ERA %.2f, WHIP %.2f, FIP %.2f",
                    p2025.getDouble("era"), p2025.getDouble("whip"), p2025.getDouble("fip"));
            age = (int) json.optDouble("age", 0);
        } catch (Exception ignored) {}

        return String.format(
                "너는 KBO 마운드의 흐름을 꿰뚫어 보는 데이터 전략가이자 수석 코치입니다. 아래 데이터를 '유일한 근거'로 하여 %s 선수의 '2026 엘리트 투수 전망 리포트'를 작성하십시오.\n\n" +

                        "[분석 기초 데이터]\n" +
                        "- 선수: %s (%d세)\n" +
                        "- 2025년 실제 성적: %s\n" +
                        "- 2026년 예측 수치:\n" +
                        "  * 엘리트 등극 확률: %.2f%%\n" +
                        "  * 보직 내 랭킹: %d위 (전체 %d명 중)\n" +
                        "  * 상위 백분위: 상위 %.2f%%\n" +
                        "- 성적 결정 핵심 지표 (반드시 본문 주어로 활용): \n%s\n\n" +

                        "[리포트 작성 및 강조 가이드 - 필독]\n" +
                        "1. **강조 표시 규칙 (필수)**: \n" +
                        "   - 본문에서 지표 명칭(예: **WHIP(이닝당 출루 허용률)**, **ERA 추세** 등)이 등장할 때마다 반드시 `**`로 감싸십시오.\n" +
                        "   - 각 섹션의 마지막에는 해당 문단의 핵심 결론 문장 하나를 통째로 `**`로 감싸 강조하십시오.\n" +
                        "2. **금지 용어**: 'SHAP', '피처', '분석 결과' 등 기계적인 단어를 절대 쓰지 마십시오. 대신 '성적 결정 요인', '도약의 근거', '지표 분석' 등으로 순화하십시오. 분석이 어렵다는 회피성 멘트도 금지입니다.\n" +
                        "3. **2025년 성적 브릿지**: ## 요약문 바로 아래, 2025년 실제 기록(%s)을 언급하며 올해의 엘리트 등극 확률과 어떻게 기술적으로 연결되는지 서술하십시오.\n" +
                        "4. **소제목 완성**: ### 소제목의 따옴표(\"\") 안에는 가이드 문구가 아닌, 데이터를 분석해서 나온 '실제 기술적 결론'을 직접 창작해 넣으십시오.\n" +
                        "5. **문단 구성**:\n" +
                        "   - [최상단]: ## [2026년 마운드 지배력을 정의하는 강력한 요약 한 줄]\n" +
                        "   - [브릿지]: (2025년 성적 리뷰 및 2026년과의 기술적 연결성 서술)\n" +
                        "   - ### 1. 2026 시즌 종합 전망: \"(리그 내 위상 및 엘리트 확률 분석 요약)\"\n" +
                        "   - ### 2. 엘리트 도약의 기술적 근거: \"(제공된 상승 기여 지표들의 시너지 요약)\"\n" +
                        "   - ### 3. 완성도를 위한 기술적 과제: \"(제공된 하락 기여 지표들로 인한 리스크 요약)\"\n" +
                        "   - ### 4. 최종 기술 종합 평론: (지표들을 총망라하여 2026년의 전술적 위상과 마운드에서의 역할을 상세 기술. 인사말 금지)\n\n" +
                        "- 말투: 냉철하고 권위 있는 전문가용 경어체(~합니다)를 사용하십시오. 이모지는 금지입니다.",
                playerName, playerName, age, perf2025,
                pred.getProbElite().doubleValue() * 100, pred.getRoleRank(), pred.getRoleTotal(), pred.getRolePercentileTop(),
                shapSummary, perf2025
        );
    }

    // 타자용 SHAP 추출
    private String extractHitterShap(String s3Context) {
        try {
            JSONObject json = new JSONObject(s3Context);
            if (!json.has("shap_explain")) return "세부 지표 데이터가 존재하지 않습니다.";

            JSONObject shap = json.getJSONObject("shap_explain");
            StringBuilder sb = new StringBuilder();
            String[] metrics = {"avg", "ops", "hr"};

            for (String m : metrics) {
                if (!shap.has(m)) continue;
                JSONObject obj = shap.getJSONObject(m);

                if (obj.has("top_positive")) {
                    sb.append("[").append(m.toUpperCase()).append(" 상승 동력]: ");
                    JSONArray pos = obj.getJSONArray("top_positive");
                    for (int i = 0; i < Math.min(3, pos.length()); i++) {
                        sb.append(metricTranslator.translate(pos.getJSONObject(i).getString("feature"))).append(", ");
                    }
                }

                if (obj.has("top_negative")) {
                    sb.append("\n[").append(m.toUpperCase()).append(" 하락 리스크]: ");
                    JSONArray neg = obj.getJSONArray("top_negative");
                    for (int i = 0; i < Math.min(3, neg.length()); i++) {
                        sb.append(metricTranslator.translate(neg.getJSONObject(i).getString("feature"))).append(", ");
                    }
                }
                sb.append("\n\n");
            }
            return sb.toString().isEmpty() ? "추출된 유의미한 지표가 없습니다." : sb.toString();
        } catch (Exception e) {
            log.error("#### [SHAP 추출 실패] 에러: {}", e.getMessage());
            return "데이터 파싱 중 오류가 발생했습니다.";
        }
    }

    // 투수용 SHAP 추출 (전체)
    private String extractPitcherShap(String s3Context) {
        try {
            JSONObject shap = new JSONObject(s3Context).getJSONObject("shap_explain").getJSONObject("elite_prob");
            StringBuilder sb = new StringBuilder();

            sb.append("[엘리트 등극 결정 요인]\n");

            sb.append("- 긍정적 기여(상승): ");
            JSONArray pos = shap.getJSONArray("top_positive");
            for (int i = 0; i < Math.min(5, pos.length()); i++) {
                sb.append(metricTranslator.translate(pos.getJSONObject(i).getString("feature"))).append(", ");
            }

            sb.append("\n- 부정적 기여(하락): ");
            JSONArray neg = shap.getJSONArray("top_negative");
            for (int i = 0; i < Math.min(5, neg.length()); i++) {
                sb.append(metricTranslator.translate(neg.getJSONObject(i).getString("feature"))).append(", ");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("#### [투수 SHAP 추출 실패] 에러: {}", e.getMessage());
            return "핵심 분석 지표 데이터 로드 실패";
        }
    }
}
