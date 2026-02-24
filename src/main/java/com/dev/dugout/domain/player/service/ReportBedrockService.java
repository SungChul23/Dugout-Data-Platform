package com.dev.dugout.domain.player.service;


import com.dev.dugout.global.common.MetricTranslator;
import com.dev.dugout.infrastructure.aws.s3.S3Service;
import com.dev.dugout.infrastructure.ml.entity.PredictionResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportBedrockService {

    private final S3Service s3Service;
    private final BedrockRuntimeClient bedrockClient; // Config에서 등록한 Bean 주입
    private final Map<String, String> playerMasterDataMap = new HashMap<>();

    private final MetricTranslator metricTranslator;

    //서버 시작 시 S3 마스터 파일을 읽어 메모리에 캐싱
    @PostConstruct
    public void init() {
        log.info("====> [초기화] S3 마스터 데이터를 메모리에 로드합니다.(선수 성적 예측)");

        // 1. 타자 데이터 로드
        loadMasterData(s3Service.fetchMasterJson(), "타자");

        // 2. 투수 데이터 로드
        loadMasterData(s3Service.fetchPitcherMasterJson(), "투수");
    }

    private void loadMasterData(String jsonContent, String type) {
        if (jsonContent != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    // 1. kbo_pcode가 있는지 먼저 확인하고, 없으면 pcode를 찾습니다.
                    String pcode;
                    if (obj.has("kbo_pcode")) {
                        pcode = String.valueOf(obj.get("kbo_pcode"));
                    } else if (obj.has("pcode")) {
                        pcode = String.valueOf(obj.get("pcode"));
                    } else {
                        // 둘 다 없는 경우 로그를 남기고 넘어갑니다.
                        log.warn("#### [{}] {}번째 데이터에 식별자(pcode)가 없습니다.", type, i);
                        continue;
                    }

                    // 전체 JSON 객체를 문자열로 저장하여 캐싱
                    playerMasterDataMap.put(pcode, obj.toString());
                }
                log.info("====> [성공] {} 명의 {} 데이터를 메모리에 캐싱했습니다.", jsonArray.length(), type);
            } catch (Exception e) {
                // 이 로그가 사용자님이 보신 ERROR 로그입니다.
                log.error("#### [초기화 실패] {} 데이터 로드 중 오류: {}", type, e.getMessage());
            }
        }
    }

    //포지션에 따라 타자 OR 투수 프롬프트를 분기 생성
    public String generatePlayerReport(PredictionResult pred) {
        String pcode = pred.getPlayer().getKboPcode();
        String s3Context = playerMasterDataMap.getOrDefault(pcode, "기본 선수 정보만 제공됨");

        String prompt;
        if ("투수".equals(pred.getPlayer().getPositionType())) {
            prompt = constructPitcherPrompt(pred, s3Context);
        } else {
            prompt = constructHitterPrompt(pred, s3Context);
        }

        return invokeBedrock(prompt);
    }

    //[타자용 프롬프트 생성]
    private String constructHitterPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        String shapSummary = extractHitterShap(s3Context); // 여기서 'MH(멀티히트 생산력)' 등이 생성됨

        String perf2025 = "데이터 확인 중";
        try {
            JSONObject json = new JSONObject(s3Context);
            JSONObject p2025 = json.getJSONObject("performance_2025");
            perf2025 = String.format("AVG(타율) %.3f, HR(홈런) %d개, OPS %.3f",
                    p2025.getDouble("avg"), p2025.getInt("hr"), p2025.getDouble("ops"));
        } catch (Exception ignored) {}

        return String.format(
                "너는 KBO 리그 데이터 분석가이자 수석 스카우터입니다. 아래 제공된 지표 데이터를 '반드시' 사용하여 %s 선수의 리포트를 작성하십시오.\n\n" +

                        "[분석 기초 데이터 - 본문 작성 시 이 명칭을 그대로 쓸 것]\n" +
                        "- 선수: %s\n" +
                        "- 2025년 성적: %s\n" +
                        "- 2026년 예측치:\n" +
                        "  * AVG(타율): %.3f (범위: %.3f ~ %.3f)\n" +
                        "  * HR(홈런): %d개 (범위: %d ~ %d)\n" +
                        "  * OPS: %.3f (범위: %.3f ~ %.3f)\n" +
                        "- 핵심 분석 지표 (본문 주어로 반드시 활용): \n%s\n\n" +

                        "[리포트 작성 및 포맷 지침 - 필독]\n" +
                        "1. **지표 명칭 강제 사용**: '핵심 분석 지표' 섹션에 제공된 명칭(예: MH(멀티히트 생산력), BB%%(볼넷률) 등)을 본문 분석 시 **토씨 하나 틀리지 말고 그대로** 사용하십시오. '분석이 불가능하다'는 식의 무책임한 문장은 절대 금지입니다.\n" +
                        "2. **최상단 메인 서머리**: 리포트 시작 시 '## '을 사용하여 2025년 성적(%s)과 2026년 전망을 꿰뚫는 날카로운 한 줄 평을 작성하십시오. 별도의 제목 태그를 붙이지 마십시오.\n" +
                        "3. **소제목 창의성**: ### 1~4번 소제목의 따옴표(\"\") 안에는 가이드 문구가 아닌, 데이터가 시사하는 실제 기술적 결론을 직접 창작해 넣으십시오.\n" +
                        "4. **기술 용어 순화**: 'SHAP', '피처'라는 단어는 절대 쓰지 말고 '지표', '동력', '원인' 등으로 표현하십시오. 이모지는 금지입니다.\n" +
                        "5. **문단 구성**:\n" +
                        "   - [최상단]: ## [2026년 기술적 운명을 결정지을 핵심 요약]\n" +
                        "   - [브릿지 문단]: 2025년 성적(%s)을 기반으로 올해의 변화 가능성을 1~2문장으로 기술.\n" +
                        "   - ### 1. 2026 시즌 전체 전망: \"(데이터 기반의 성적 추세 요약)\"\n" +
                        "   - ### 2. 성장을 이끄는 기술적 동력: \"(제공된 상승 지표가 만들어낼 효과 요약)\"\n" +
                        "   - ### 3. 주의해야 할 변수와 리스크: \"(제공된 하락 지표로 인한 리스크 요약)\"\n" +
                        "   - ### 4. 최종 기술 종합 평론: (섹션 1~3을 총망라하여 전문적으로 서술. 웅장하고 상세하게 작성하되 팬 서비스 멘트는 제외)\n\n" +
                        "- 말투: 냉철한 전문가용 경어체(~합니다)를 사용하고 강조는 ** 강조할 내용 ** 형식을 지키십시오.",
                playerName, playerName, perf2025,
                pred.getPredAvg(), pred.getAvgMin(), pred.getAvgMax(),
                pred.getPredHr(), pred.getHrMin(), pred.getHrMax(),
                pred.getPredOps(), pred.getOpsMin(), pred.getOpsMax(),
                shapSummary, perf2025, perf2025
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
        } catch (Exception ignored) {
        }

        return String.format(
                "너는 KBO 마운드의 흐름을 꿰뚫어 보는 데이터 전략가이자 수석 코치입니다. 아래 데이터를 바탕으로 %s 선수의 '2026 엘리트 투수 전망 리포트'를 작성하십시오.\n\n" +

                        "[스카우팅 기초 데이터]\n" +
                        "- 선수: %s (%d세)\n" +
                        "- 2025년 기록: %s\n" +
                        "- 2026년 예측 수치:\n" +
                        "  * 엘리트 등극 확률: %.2f%%\n" +
                        "  * 보직 내 랭킹: %d위 (전체 %d명 중)\n" +
                        "  * 상위 백분위: 상위 %.2f%%\n" +
                        "- 주요 분석 핵심 지표 (SHAP): \n%s\n\n" +

                        "[리포트 작성 및 포맷 지침 - 필독]\n" +
                        "1. **이모지 사용 금지**: 리포트 전체에서 이모티콘이나 이모지를 절대 사용하지 마십시오.\n" +
                        "2. **최상단 메인 요약 (가장 크게)**: 리포트 시작 시 '## 2026년, 리그를 지배할 엘리트 투수로의 도약이 예견되는 %s'와 같이 '## '을 사용하여 가장 큰 글씨로 한 줄 요약을 작성하십시오.\n" +
                        "3. **섹션 소제목 (중간 크기)**: 모든 소제목은 반드시 '### 숫자. 섹션명: \"데이터 기반의 통찰\"' 형식을 사용하십시오. 반드시 제목 앞에 '### '을 붙여야 합니다.\n" +
                        "4. **강조 표시 활용**: 핵심 수치(예: ERA, WHIP, 엘리트 확률 등)와 리포트의 결론이 되는 핵심 문장은 반드시 ** 강조할 내용 ** 처럼 별표 앞뒤에 공백을 두어 강조하십시오.\n" +
                        "5. **데이터 기반 기술 분석**: '정신력', '노력' 같은 일반론은 배제하십시오. 대신 ERA_trend, WHIP, 연령(Age), 구위 지표 등 제공된 SHAP 요인을 바탕으로 냉철하게 분석하십시오.\n" +
                        "6. **문단 구성**:\n" +
                        "   - [최상단]: ## 한 줄 메인 요약 (데이터 통찰 중심)\n" +
                        "   - ### 1. 2026 시즌 종합 전망: \"리그 전체 상위 %.2f%%에 해당하는 압도적 위상\"\n" +
                        "   - ### 2. 엘리트 도약의 기술적 근거: \"안정적인 WHIP 관리와 구위 지표의 조화\"\n" +
                        "   - ### 3. 완성도를 위한 기술적 과제: \"ERA 변동성 억제와 지표의 일관성 확보\"\n" +
                        "   - ### 4. 최종 결론: \"데이터는 그가 2026년 마운드의 새로운 지배자가 될 것임을 확신합니다\"\n\n" +
                        "- 말투: 단호하고 열정적이면서도 사실에 기반한 전문가적 경어체(~합니다)를 사용하십시오.",
                playerName, playerName, age, perf2025,
                pred.getProbElite().doubleValue() * 100, pred.getRoleRank(), pred.getRoleTotal(), pred.getRolePercentileTop(),
                shapSummary, playerName, pred.getRolePercentileTop()
        );
    }

    // 타자용 SHAP 추출 (상위 2개)
    private String extractHitterShap(String s3Context) {
        try {
            JSONObject shap = new JSONObject(s3Context).getJSONObject("shap_explain");
            StringBuilder sb = new StringBuilder();
            String[] metrics = {"avg", "ops", "hr"};

            for (String m : metrics) {
                if (!shap.has(m)) continue;
                JSONObject obj = shap.getJSONObject(m);
                sb.append("[").append(m.toUpperCase()).append(" 결정 요인]\n");

                sb.append("- 성적 상승 기여: ");
                JSONArray pos = obj.getJSONArray("top_positive");
                for (int i = 0; i < Math.min(2, pos.length()); i++) {
                    sb.append(metricTranslator.translate(pos.getJSONObject(i).getString("feature"))).append(", ");
                }

                sb.append("\n- 성적 하락 기여: ");
                JSONArray neg = obj.getJSONArray("top_negative");
                for (int i = 0; i < Math.min(2, neg.length()); i++) {
                    sb.append(metricTranslator.translate(neg.getJSONObject(i).getString("feature"))).append(", ");
                }
                sb.append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "핵심 지표 데이터 분석 불가";
        }
    }

    // 투수용 SHAP 추출 (전체)
    private String extractPitcherShap(String s3Context) {
        try {
            JSONObject shap = new JSONObject(s3Context).getJSONObject("shap_explain").getJSONObject("elite_prob");
            StringBuilder sb = new StringBuilder();
            sb.append("상승 요인: ");
            JSONArray pos = shap.getJSONArray("top_positive");
            for (int i = 0; i < pos.length(); i++) sb.append(pos.getJSONObject(i).getString("feature")).append(" ");
            sb.append("\n하락 요인: ");
            JSONArray neg = shap.getJSONArray("top_negative");
            for (int i = 0; i < neg.length(); i++) sb.append(neg.getJSONObject(i).getString("feature")).append(" ");
            return sb.toString();
        } catch (Exception e) {
            return "SHAP 데이터 추출 중 오류";
        }
    }

    //캐싱된 데이터를 찾아 베드락에게 전달
    private String invokeBedrock(String prompt) {
        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 1500);
        payload.put("temperature", 0.7);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        try {
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] 리포트 생성 실패: {}", e.getMessage());
            return "AI 리포트 생성 중 오류가 발생했습니다.";
        }
    }
}