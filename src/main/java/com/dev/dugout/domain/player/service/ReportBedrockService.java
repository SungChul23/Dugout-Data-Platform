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

        // 2025년 상세 실적 및 예측 보조 데이터 추출 (JSON 파싱 강화)
        String detailedContext = "데이터 분석 중";
        try {
            JSONObject json = new JSONObject(s3Context);
            JSONObject p2025 = json.getJSONObject("performance_2025");

            // 리포트의 풍성함을 위해 더 많은 지표 포함
            detailedContext = String.format(
                    "2025 실적: 타율 %.3f, 출루율 %.3f, 장타율 %.3f, OPS %.3f, 홈런 %d개\n" +
                            "특이사항: %s",
                    p2025.optDouble("avg"), p2025.optDouble("obp"), p2025.optDouble("slg"),
                    p2025.optDouble("ops"), p2025.optInt("hr"),
                    "멀티히트 및 결승타 등 클러치 상황에서의 기여도 높음" // 필요 시 JSON에서 추출
            );
        } catch (Exception ignored) {}

        String shapSummary = extractHitterShap(s3Context);

        return String.format(
                "너는 KBO 리그의 전설적인 스카우터이자 최고의 데이터 분석 전문가입니다. " +
                        "아래의 정밀 데이터를 분석하여 %s 선수의 '2026 시즌 프리뷰 정밀 리포트'를 작성하십시오.\n\n" +

                        "[분석 대상 데이터]\n" +
                        "- 선수명: %s\n" +
                        "- %s\n" +
                        "- 2026 예측 지표 (중앙값 및 퍼포먼스 범위):\n" +
                        "  1) 타율(AVG): 예측 %.3f (최저 %.3f ~ 최고 %.3f)\n" +
                        "  2) 출루율(OBP): 예측 %.3f (최저 %.3f ~ 최고 %.3f)\n" +
                        "  3) 장타율(SLG): 예측 %.3f (최저 %.3f ~ 최고 %.3f)\n" +
                        "  4) OPS: 예측 %.3f (최저 %.3f ~ 최고 %.3f)\n" +
                        "  5) 홈런(HR): 예측 %d개 (최저 %d ~ 최고 %d)\n" +
                        "- 모델 분석 동력(상승/하락 요인 데이터): \n%s\n\n" +

                        "[리포트 작성 가이드라인]\n" +
                        "1. **도입부 (2025 시즌 리뷰)**: '1. 2025 시즌 리뷰: \"문구\"' 형식으로 시작하십시오. 선수의 위상을 상징하는 강렬한 수식어를 사용하고, 문구는 반드시 **볼드체**로 강조하며 작년 성적이 팀에 준 임팩트를 서술하십시오.\n" +
                        "2. **퍼포먼스 가이드 (Performance Range)**: 예측치의 '범위(Min~Max)'에 집중하십시오. '최악의 시나리오에서도 이 정도는 해준다'는 안정감과 '터지면 이 정도까지 간다'는 고점을 스토리텔링으로 풀어내십시오.\n" +
                        "3. **동력 및 리스크 분석**: 상승 동력은 🚀, 하락 리스크는 ⚠️ 이모지를 사용하십시오. 단어(예: **체력 과부하**, **결승타 능력**)에 **볼드체**를 적용하십시오.  수치를 나열하지 말고 '체력 과부하', '선구안의 완성' 등 야구적인 언어로 해석하며 핵심 원인이 되는 단어(예: **체력 과부하**, **결승타 능력**)에 **볼드체**를 적용하십시오..\n" +
                        "4. **핵심 인사이트 (Deep Dive)**: 데이터 중 가장 흥미로운 지표(예: BABIP, 삼진율 등) 하나를 골라 '이것은 운이 아니라 실력'임을 강조하거나 반등의 열쇠임을 짚어주십시오. 또한 **볼드체**를 결합하여 강렬하게 전달하십시오.\n" +
                        "5. **전략적 권고 및 총평**: 구단 프런트나 감독에게 제언하는 '최종 권고 사항'을 포함하고, 마지막은 '2026 시즌 총평: \"문구\"'와 함께 선수의 가치를 정의하며 총평 문구 역시 **볼드체**로 강조하며 마무리하십시오.\n\n" +

                        "[주의 사항]\n" +
                        "- 모든 소제목과 핵심 결론, 그리고 수치 데이터는 **볼드체(**...**)**를 적극적으로 사용하여 사용자가 한눈에 핵심을 파악할 수 있게 하십시오.\n" +
                        "- 'SHAP', '모델', '피처', '앙상블' 같은 데이터 과학 용어는 절대 사용하지 마십시오.\n" +
                        "- 말투: 전문가의 신뢰감이 느껴지면서도 현장감이 살아있는 열정적인 경어체(~합니다)를 사용하십시오.\n" +
                        "- 가독성을 위해 소제목과 볼드(**)를 적극적으로 활용하십시오.",

                playerName, playerName, detailedContext,
                pred.getPredAvg(), pred.getAvgMin(), pred.getAvgMax(),
                pred.getPredObp(), pred.getObpMin(), pred.getObpMax(), // OBP/SLG 변수명은 환경에 맞게 조정하세요
                pred.getPredSlg(), pred.getSlgMin(), pred.getSlgMax(),
                pred.getPredOps(), pred.getOpsMin(), pred.getOpsMax(),
                pred.getPredHr(), pred.getHrMin(), pred.getHrMax(),
                shapSummary
        );
    }

    // [투수용 프롬프트 생성]
    private String constructPitcherPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        String shapSummary = extractPitcherShap(s3Context); // 엘리트 확률 원인 전체

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
                "너는 KBO 마운드의 흐름을 꿰뚫어 보는 수석 코치입니다. 아래 데이터를 바탕으로 %s 선수의 '2026 엘리트 투수 전망 리포트'를 작성하십시오.\n\n" +

                        "[스카우팅 기초 데이터]\n" +
                        "- 선수: %s (%d세)\n" +
                        "- 2025년 기록: %s\n" +
                        "- 2026년 예측 수치:\n" +
                        "  * 엘리트 등극 확률: %.2f%%\n" +
                        "  * 보직 내 랭킹: %d위 (전체 %d명 중)\n" +
                        "  * 상위 백분위: 상위 %.2f%%\n" +
                        "- 주요 분석 요인 (SHAP): \n%s\n\n" +

                        "[리포트 작성 지침 - 필독]\n" +
                        "1. **첫 줄 도입부**: '마운드의 지배자', '철벽의 재강림' 등 팬들이 전율할 만한 호칭으로 시작하십시오.\n" +
                        "2. **구조화된 소제목**: '1. 2026 시즌 마운드 전망: \"압도적 지배력\"' 처럼 숫자로 시작하고 임팩트 있는 소제목을 붙이십시오.\n" +
                        "3. **랭킹의 의미 강조**: 보직 내 %d위라는 수치가 리그 전체에서 어떤 위상을 가지는지 스카우터의 시각에서 극찬하거나 분석하십시오.\n" +
                        "4. **기술적 용어 금지**: '엘리트 스크리닝', 'SHAP', '베이스라인' 등의 단어 대신 '성장의 지표', '도약의 근거' 등의 표현을 사용하십시오.\n" +
                        "5. **문단 구성**:\n" +
                        "   - 1섹션: 2026 시즌 종합 전망 (엘리트 확률과 랭킹의 의미)\n" +
                        "   - 2섹션: 엘리트 도약의 근거 (상승 요인 기반 분석)\n" +
                        "   - 3섹션: 완성도를 위한 과제 (하락 요인 기반 분석)\n" +
                        "   - 4섹션: 최종 결론 (2026년 이 투수가 선사할 '뽕맛' 요약)\n\n" +
                        "- 말투: 단호하면서도 열정적인 전문가의 어조(~합니다)를 사용하십시오.",
                playerName, playerName, age, perf2025,
                pred.getProbElite().doubleValue() * 100, pred.getRoleRank(), pred.getRoleTotal(), pred.getRolePercentileTop(),
                shapSummary, pred.getRoleRank()
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
        payload.put("max_tokens", 1200);
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