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
        log.info("====> [초기화] S3 마스터 데이터를 메모리에 로드합니다.");

        // 1. 타자 데이터 로드
        loadMasterData(s3Service.fetchMasterJson(), "타자");

        // 2. 투수 데이터 로드 (S3Service에 투수용 메서드 추가 필요)
        loadMasterData(s3Service.fetchPitcherMasterJson(), "투수");
    }

    private void loadMasterData(String jsonContent, String type) {
        if (jsonContent != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    // JSON 내 키값이 pcode 또는 player_id일 수 있으므로 유연하게 처리
                    String pcode = obj.has("pcode") ? String.valueOf(obj.get("pcode")) : String.valueOf(obj.get("player_id"));
                    playerMasterDataMap.put(pcode, obj.toString());
                }
                log.info("====> [성공] {} 명의 {} 데이터를 캐싱했습니다.", jsonArray.length(), type);
            } catch (Exception e) {
                log.error("#### [초기화 실패] {} JSON 파싱 오류: {}", type, e.getMessage());
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
        // 홈런 수치를 정수로 변환 (예: 15.3 -> 15)
        int hrVal = (int) Math.round(pred.getPredHr().doubleValue());
        int hrDiff = (int) Math.round(pred.getHrDiff().doubleValue());
        String playerName = pred.getPlayer().getName();

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터야. " +
                        "아래 지침에 따라 %s 선수의 리포트를 작성해줘.\n\n" +

                        "### [분석 데이터] ###\n" +
                        "1. 선수 맥락 (S3): %s\n" +
                        "2. 2026 예측 성적:\n" +
                        "   - 타율: %.3f (변화: %.3f)\n" +
                        "   - 홈런: %d개 (변화: %d)\n" +
                        "   - OPS: %.3f (변화: %.3f)\n\n" +

                        "### [리포트 작성 규칙 - 수석 스카우터의 명령] ###\n" +
                        "1. **타이틀 형식**: 리포트의 첫 줄은 반드시 [2026 시즌 분석 리포트 - %s 선수] 로 시작할 것.\n" +
                        "2. **기호 사용 금지**: '#', '*', '-' 같은 마크다운 서식 기호를 절대 사용하지 말 것. 오직 텍스트와 줄바꿈으로만 구성할 것.\n" +
                        "3. **항목별 분석**: 타율, 홈런, OPS 순서로 문단을 나누어 분석하되, 'age_2026' 수치를 근거로 들어 에이징 커브 관점에서 설명할 것.\n" +
                        "4. **종합 평가**: 마지막 문단에는 선수의 팀 내 역할과 향후 전망에 대한 총평을 남길 것.\n" +
                        "5. **수치 처리**: 홈런은 반드시 내가 제공한 정수(%d개)로만 언급할 것.\n",
                playerName,
                s3Context,
                pred.getPredAvg(), pred.getAvgDiff(),
                hrVal, hrDiff,
                pred.getPredOps(), pred.getOpsDiff(),
                playerName, // 타이틀용
                hrVal // 정수 홈런용
        );
    }

    // [투수용 프롬프트 생성]
    private String constructPitcherPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        double eraProb = pred.getEraEliteProb() != null ? pred.getEraEliteProb().doubleValue() * 100 : 0;
        double whipProb = pred.getWhipEliteProb() != null ? pred.getWhipEliteProb().doubleValue() * 100 : 0;

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터야. %s 투수의 리포트를 작성해줘.\n\n" +

                        "### [분석 데이터] ###\n" +
                        "1. 선수 맥락 (S3): %s\n" +
                        "2. 현재 상태: ERA %.2f, WHIP %.2f\n" +
                        "3. 2026 엘리트 등극 확률(ML 예측): ERA 상위 20%% 진입 %.1f%%, WHIP 상위 20%% 진입 %.1f%%\n\n" +

                        "### [리포트 작성 규칙 - 수석 스카우터의 명령] ###\n" +
                        "1. **타이틀 형식**: 리포트의 첫 줄은 반드시 [2026 시즌 분석 리포트 - %s 선수] 로 시작할 것.\n" +
                        "2. **기호 사용 금지**: '#', '*', '-' 같은 마크다운 서식 기호를 절대 사용하지 말 것. 오직 텍스트와 줄바꿈으로만 구성할 것.\n" +
                        "3. **보직 중립성**: 선발, 불펜 등 특정 보직을 단정 짓지 말 것. 대신 '마운드의 핵심 전력', '팀의 주축 자원', '필승조 혹은 핵심 로테이션'과 같은 포괄적인 표현을 사용할 것.\n" +
                        "4. **지표의 신뢰도**: 현재의 낮은 수치(ERA, WHIP)가 내년에도 유지될 확률이 매우 높다는 점을 '성적의 지속 가능성' 측면에서 분석할 것.\n" +
                        "5. **에이징 커브 분석**: 20대 초중반의 나이가 가지는 성장의 여지와 신체적 전성기 진입 단계를 결합하여 설명할 것.\n" +
                        "6. **종합 평가**: 마지막 문단에는 이 투수가 팀의 전체적인 마운드 뎁스(Depth)와 승률 계산에 기여할 전략적 가치를 총평할 것.\n",
                playerName, s3Context,
                pred.getPredEra(), pred.getPredWhip(),
                eraProb, whipProb,
                playerName
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