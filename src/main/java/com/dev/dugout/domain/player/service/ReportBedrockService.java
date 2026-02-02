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

                    // pcode로 식별자 통일 (타입 안정성을 위해 String.valueOf 사용)
                    String pcode = String.valueOf(obj.get("pcode"));

                    // 전체 JSON 객체를 문자열로 저장하여 Bedrock에게 Full Context 제공
                    playerMasterDataMap.put(pcode, obj.toString());
                }
                log.info("====> [성공] {} 명의 {} 데이터를 메모리에 캐싱했습니다.", jsonArray.length(), type);
            } catch (Exception e) {
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
        // 홈런 수치를 정수로 변환
        int hrVal = (int) Math.round(pred.getPredHr().doubleValue());
        int hrDiff = (int) Math.round(pred.getHrDiff().doubleValue());
        String playerName = pred.getPlayer().getName();

        // JSON에서 타자 전용 키 "age_2026" 추출
        int age2026 = 0;
        try {
            JSONObject json = new JSONObject(s3Context);
            age2026 = json.has("age_2026") ? json.getInt("age_2026") : 0;
        } catch (Exception e) {
            log.error("타자 나이 파싱 실패: {}", e.getMessage());
        }

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터입니다. "
                        + "아래 지침에 따라 %s 선수의 2026 시즌 타자 분석 리포트를 작성하십시오.\n\n" + // 1. %s

                        "[2026 시즌 분석 리포트 - %s 선수 (%d세)]\n\n" + // 2. %s, 3. %d

                        "본 리포트는 제공된 수치와 과거 맥락 정보를 바탕으로 해당 선수가 2026 시즌 "
                        + "팀의 핵심 타격 자원으로 평가되기에 충분한지 스카우터의 관점에서 검토하는 것을 목표로 합니다.\n\n" +

                        "분석 활용 데이터:\n" +
                        "선수 맥락 정보(S3): %s\n" + // 4. %s
                        "2026 시즌 성적 전망 수치: "
                        + "타율 %.3f(변화 %.3f), 홈런 %d개(변화 %d), OPS %.3f(변화 %.3f)\n\n" + // 5~10

                        "리포트 작성 규칙:\n" +
                        "- 모든 문장은 '~합니다', '~입니다'와 같은 정중한 경어체로 작성하십시오.\n" +
                        "- 고정 제목([2026 시즌 분석 리포트 - %s 선수 (%d세)]) 이후 별도의 소제목 없이 바로 본문을 작성하십시오.\n" + // 11. %s, 12. %d
                        "- 마크다운 기호나 특수 기호는 사용하지 말고 텍스트와 줄바꿈만으로 구성하십시오.\n" +
                        "- 분량은 총 3문단으로 제한하며, 각 문단은 서로 다른 분석 목적을 가져야 합니다.\n\n" +

                        "분석 구성 흐름:\n" +
                        "첫 번째 문단에서는 타율과 OPS 변화를 중심으로 'history' 데이터에 나타난 최근 3~5년간의 타격 생산성 추이를 분석하십시오. "
                        + "과거의 지표 흐름과 비교했을 때 2026년 예측치가 가지는 객관적인 경쟁력을 평가하십시오.\n\n" +

                        "두 번째 문단에서는 과거 기록 중 '커리어 하이(Career High)' 성적을 찾아내어 2026년 예측치(%d개 홈런, %.3f OPS)와 대조하십시오. " + // 13. %d, 14. %.3f
                        "이와 함께 2026년 나이(age_2026)를 참고하되, 데이터 흐름에 따라 '에이징 커브에 의한 변화', '기량 유지 및 노련함', '유망주의 도약' 등 "
                        + "이 선수에게 가장 적합한 상황을 스스로 판단하여 성적 유지 및 달성 가능성을 심층 분석하십시오.\n\n" +

                        "세 번째 문단에서는 앞선 분석을 종합하여 이 선수가 2026 시즌 "
                        + "확정적인 중심 타선 자원인지, 혹은 조건부 핵심 타자로 분류하는 것이 타당한지를 "
                        + "팀 내 역할과 함께 명확히 정리하십시오.\n",

                // 인자 순서 매핑
                playerName,             // 1. 지침 시작
                playerName, age2026,    // 2, 3. 상단 제목 (이름, 나이)
                s3Context,              // 4. 맥락 정보
                pred.getPredAvg(), pred.getAvgDiff(), // 5, 6. 타율 관련
                hrVal, hrDiff,          // 7, 8. 홈런 관련
                pred.getPredOps(), pred.getOpsDiff(), // 9, 10. OPS 관련
                playerName, age2026,    // 11, 12. 규칙 내 제목 (이름, 나이)
                hrVal,                  // 13. 2문단 홈런 수치
                pred.getPredOps()       // 14. 2문단 OPS 수치
        );
    }

    // [투수용 프롬프트 생성]
    private String constructPitcherPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();

        // 확률 수치 변환 (0.49 -> 49.0)
        double eraProb = pred.getEraEliteProb() != null ? pred.getEraEliteProb().doubleValue() * 100 : 0;
        double whipProb = pred.getWhipEliteProb() != null ? pred.getWhipEliteProb().doubleValue() * 100 : 0;

        // 1. JSON에서 투수 전용 키 "age" 추출
        int age = 0;
        try {
            JSONObject json = new JSONObject(s3Context);
            // 투수는 age가 실수(25.0)로 들어오는 경우가 많으므로 getDouble 후 변환
            age = json.has("age") ? (int) json.getDouble("age") : 0;
        } catch (Exception e) {
            log.error("투수 나이 파싱 실패: {}", e.getMessage());
        }

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터입니다. "
                        + "아래 지침에 따라 %s 선수의 투수 분석 리포트를 작성하십시오.\n\n" + // 1. playerName

                        "[2026 시즌 분석 리포트 - %s 선수 (%d세)]\n\n" + // 2. playerName, 3. age

                        "분석 활용 데이터:\n" +
                        "- 선수 통합 컨텍스트(JSON): %s\n" + // 4. s3Context
                        "- 2026 시즌 성적 전망: ERA %.2f, WHIP %.2f\n" + // 5. predEra, 6. predWhip
                        "- 엘리트 달성 확률: ERA 상위권 %.1f%%, WHIP 상위권 %.1f%%\n\n" + // 7. eraProb, 8. whipProb

                        "리포트 작성 규칙:\n" +
                        "- 모든 문장은 정중한 경어체(~합니다)로 작성하십시오.\n" +
                        "- 고정 제목([2026 시즌 분석 리포트 - %s 선수]) 이후 소제목 없이 바로 본문을 작성하십시오.\n" + // 9. playerName
                        "- 마크다운이나 특수 기호 없이 텍스트와 줄바꿈으로만 구성하십시오.\n" +
                        "- 총 3문단으로 구성하며, 각 문단은 아래의 분석 목적을 반드시 달성해야 합니다.\n\n" +

                        "분석 구성 흐름:\n" +
                        "첫 번째 문단에서는 JSON 내 'history'를 바탕으로 최근 3~5년간의 지표(ERA, WHIP, SO) 변화 추이를 분석하십시오. "
                        + "현재의 예측치가 과거의 상승/하락 궤적과 비교했을 때 얼마나 타당한지 객관적으로 평가하십시오.\n\n" +

                        "두 번째 문단에서는 이 선수의 특성에 맞는 [맞춤형 데이터 검증]을 수행하십시오. "
                        + "만약 과거 성적 중 커리어 하이가 있다면 예측치와 대조하고, 부상 이력이 보인다면 재기 가능성을, "
                        + "나이가 많다면 에이징 커브를, 유망주라면 성장 잠재력을 분석하십시오. "
                        + "반드시 JSON 데이터를 근거로 이 선수에게 가장 핵심적인 변화 요인이 무엇인지 포착하여 기술하십시오.\n\n" +

                        "세 번째 문단에서는 앞선 분석을 종합하여 2026 시즌 이 선수가 팀 마운드에서 맡아야 할 전략적 역할과 "
                        + "확정적 주전 혹은 조건부 전력 여부에 대한 최종 스카우팅 결론을 내리십시오.\n",

                // 인자 순서 매핑 (정확히 9개)
                playerName,             // 1
                playerName, age,        // 2, 3 (제목용 이름과 나이)
                s3Context,              // 4 (JSON 컨텍스트)
                pred.getPredEra(),      // 5
                pred.getPredWhip(),     // 6
                eraProb,                // 7
                whipProb,               // 8
                playerName              // 9 (규칙 내 제목용 이름)
        );
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