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
        String jsonContent = s3Service.fetchMasterJson();

        if (jsonContent != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    // JSON의 player_id(pcode)를 String 키로 저장
                    String pcode = String.valueOf(obj.get("player_id"));
                    playerMasterDataMap.put(pcode, obj.toString());
                }
                log.info("====> [성공] 총 {}명의 선수 데이터를 캐싱했습니다.", playerMasterDataMap.size());
            } catch (Exception e) {
                log.error("#### [초기화 실패] JSON 파싱 중 오류: {}", e.getMessage());
            }
        }
    }

    //정교한 프롬프트를 생성
    private String constructPrompt(PredictionResult pred, String s3Context) {
        return String.format(
                "너는 KBO 전문 세이버메트릭스 분석가이자 스카우터야. 아래 [S3 마스터 데이터]와 [DB 예측 지표]를 종합하여 %s 선수의 2026년 시즌 전망 리포트를 작성해줘.\n\n" +
                        "### 1. S3 마스터 데이터 (상세 맥락) ###\n%s\n\n" +
                        "### 2. DB 예측 지표 ###\n" +
                        "- 2026 예측 타율: %.3f (변화: %.3f)\n" +
                        "- 2026 예측 홈런: %d개 (변화: %d)\n" +
                        "- 2026 예측 OPS: %.3f (변화: %.3f)\n\n" +
                        "### 리포트 작성 규칙 ###\n" +
                        "1. **데이터 기반**: S3에 담긴 과거 성적과 26년 예측치의 변화를 기술적으로 분석할 것.\n" +
                        "2. **전문적 어조**: 신뢰감 있고 간결한 문체를 사용할 것.\n" +
                        "3. **한국어 요약**: 반드시 한국어로 3문장 이내로 작성할 것.\n",
                pred.getPlayer().getName(),
                s3Context, // S3에서 가져온 풍부한 정보 주입
                pred.getPredAvg(), pred.getAvgDiff(),
                pred.getPredHr(), pred.getHrDiff(),
                pred.getPredOps(), pred.getOpsDiff()
        );
    }

    //캐싱된 데이터를 찾아 베드락에게 전달
    public String generatePlayerReport(PredictionResult pred) {
        // 엔티티의 kboPcode(String)로 S3 데이터 조회
        String pcode = pred.getPlayer().getKboPcode();
        String s3Context = playerMasterDataMap.getOrDefault(pcode, "기본 선수 정보만 제공됨");

        String prompt = constructPrompt(pred, s3Context);

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 500);
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

            // 변수명 수정: client -> bedrockClient
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");

        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] 선수 분석 실패: {}", e.getMessage());
            return "AI 리포트 생성 중 오류가 발생했습니다. 수치 지표를 확인해 주세요.";
        }
    }
}