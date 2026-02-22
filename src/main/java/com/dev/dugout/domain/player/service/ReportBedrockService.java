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
                "너는 KBO 리그의 전설적인 스카우터이자 데이터 분석가입니다. 아래 데이터를 바탕으로 %s 선수의 '2026 시즌 프리뷰 리포트'를 작성하십시오.\n\n" +

                        "[스카우팅 기초 데이터]\n" +
                        "- 선수: %s\n" +
                        "- 2025년 기록: %s\n" +
                        "- 2026년 예측 수치:\n" +
                        "  * 타율: %.3f (예측 범위: %.3f ~ %.3f)\n" +
                        "  * 홈런: %d개 (예측 범위: %d ~ %d)\n" +
                        "  * OPS: %.3f (예측 범위: %.3f ~ %.3f)\n" +
                        "- 주요 분석 요인 (SHAP): \n%s\n\n" +

                        "[리포트 작성 지침 - 필독]\n" +
                        "1. **첫 줄 도입부**: 선수의 현재 위상을 상징하는 '강렬한 수식어'와 호칭으로 시작하여 팬들의 기대감을 고조시키십시오.\n" +
                        "2. **구조화된 소제목**: 각 섹션은 '1. 2026 시즌 타율 전망: \"문구\"' 처럼 숫자로 시작하고 데이터의 의미를 담은 매력적인 소제목을 붙이십시오.\n" +
                        "3. **데이터의 스토리텔링**: 수치를 단순히 나열하지 마십시오. 'BABIP이 낮다'면 '불운을 뚫고 타격감을 유지했다'는 식으로, '삼진율이 높다'면 '공격적인 성향이 장타력을 이끈다'는 식으로 해석하십시오.\n" +
                        "4. **기술 용어 절대 금지**: '앙상블 모델', 'SHAP', '피처' 등 기계적인 단어 대신 '분석 결과', '상승의 원동력', '주의해야 할 변수' 등으로 표현하십시오.\n" +
                        "5. **문단 구성**:\n" +
                        "   - 1섹션: 2026 전체 전망 (전년 대비 변화와 예측 범위 언급)\n" +
                        "   - 2섹션: 성장을 이끄는 핵심 동력 (상승 요인 기반 분석)\n" +
                        "   - 3섹션: 경계해야 할 변수 (하락 요인 기반 분석)\n" +
                        "   - 4섹션: 최종 결론 (팬들에게 주는 확신)\n\n" +
                        "- 말투: 전문가답고 열정적인 경어체(~합니다)를 사용하십시오.",
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
        payload.put("max_tokens", 1000);
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