package com.dev.dugout.domain.player.service;


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
        String shapSummary = extractHitterShap(s3Context);

        // 2025년 실적 추출
        String perf2025 = "데이터 확인 중";
        try {
            JSONObject json = new JSONObject(s3Context);
            JSONObject p2025 = json.getJSONObject("performance_2025");
            perf2025 = String.format("타율 %.3f, 홈런 %d개, OPS %.3f",
                    p2025.getDouble("avg"), p2025.getInt("hr"), p2025.getDouble("ops"));
        } catch (Exception ignored) {
        }

        return String.format(
                "너는 KBO 리그 데이터 분석가이자 수석 스카우터입니다. 아래 제공된 SHAP 데이터를 기반으로 %s 선수의 '2026 시즌 프리뷰 리포트'를 작성하십시오.\n\n" +

                        "[스카우팅 기초 데이터]\n" +
                        "- 선수: %s\n" +
                        "- 2025년 기록: %s\n" +
                        "- 2026년 예측 수치:\n" +
                        "  * 타율: %.3f (범위: %.3f ~ %.3f)\n" +
                        "  * 홈런: %d개 (범위: %d ~ %d)\n" +
                        "  * OPS: %.3f (범위: %.3f ~ %.3f)\n" +
                        "- 분석 핵심 지표 (SHAP 데이터): \n%s\n\n" +

                        "[리포트 작성 지침 - 필독]\n" +
                        "1. **중복 표현 금지**: '조정기', '안착할 전망', '견고한 타격 메커니즘' 등 특정 문구를 모든 선수에게 반복하지 마십시오. 데이터의 성격에 따라 매번 새로운 통찰을 제시하십시오.\n" +
                        "2. **최상단 메인 서머리**: 리포트 시작 시 '## '을 사용하여 해당 선수의 2026년 운명을 결정지을 가장 핵심적인 지표(SHAP 기반)를 요약한 강렬한 한 줄을 작성하십시오.\n" +
                        "3. **창의적인 소제목**: 각 섹션의 제목은 반드시 '### 숫자. 섹션명: \"데이터의 핵심을 담은 개성 있는 문구\"' 형식을 사용하십시오. 예시 문구를 그대로 베끼지 마십시오.\n" +
                        "4. **SHAP 데이터의 적극적 해석**: 제공된 SHAP 데이터(상승/하락 요인)에 있는 지표들(MH, BB%%, BABIP, GO/AO, Age, HR_trend 등)을 본문 분석의 핵심 근거로 삼으십시오. 지표의 이름만 나열하지 말고 그 지표가 왜 성적 변화를 일으키는지 분석가답게 설명하십시오.\n" +
                        "5. **강조 구문**: 핵심 지표와 결정적 문장은 ** 강조할 내용 ** 처럼 작성하십시오.\n" +
                        "6. **문단 구성 가이드**:\n" +
                        "   - [최상단]: ## [선수의 2026년 핵심 키워드를 담은 한 줄 요약]\n" +
                        "   - ### 1. 2026 시즌 전체 전망: [전년 대비 성적 변화의 폭과 예측 범위의 의미를 데이터 중심으로 기술]\n" +
                        "   - ### 2. 성장을 이끄는 핵심 동력: [SHAP 상승 요인(Positive) 지표들을 바탕으로 한 기술적 강점 분석]\n" +
                        "   - ### 3. 주의해야 할 변수와 리스크: [SHAP 하락 요인(Negative) 지표들을 바탕으로 성적 하락의 원인이나 주의점 분석]\n" +
                        "   - ###### 4. 최종 기술 종합 평론: [섹션 1~3의 지표들을 총망라하여 2026년 선수의 기술적 위상을 상세하게 서술 (간단히 요약하지 말 것)]\n\n" +
                        "- 말투: 냉철하고 전문적인 분석가용 경어체(~합니다)를 사용하십시오. 이모지는 절대 금지입니다.",
                playerName, playerName, perf2025,
                pred.getPredAvg(), pred.getAvgMin(), pred.getAvgMax(),
                pred.getPredHr(), pred.getHrMin(), pred.getHrMax(),
                pred.getPredOps(), pred.getOpsMin(), pred.getOpsMax(),
                shapSummary
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
                sb.append("[").append(m.toUpperCase()).append("] 상승요인: ");
                JSONArray pos = obj.getJSONArray("top_positive");
                for (int i = 0; i < Math.min(2, pos.length()); i++)
                    sb.append(pos.getJSONObject(i).getString("feature")).append(" ");
                sb.append("/ 하락요인: ");
                JSONArray neg = obj.getJSONArray("top_negative");
                for (int i = 0; i < Math.min(2, neg.length()); i++)
                    sb.append(neg.getJSONObject(i).getString("feature")).append(" ");
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "SHAP 데이터 요약 중 오류";
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