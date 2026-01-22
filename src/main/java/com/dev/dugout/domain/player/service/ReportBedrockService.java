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
        // 홈런 수치를 정수로 변환
        int hrVal = (int) Math.round(pred.getPredHr().doubleValue());
        int hrDiff = (int) Math.round(pred.getHrDiff().doubleValue());

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터야. " +
                        "제공된 데이터를 바탕으로 %s 선수의 '2026 시즌 분석 리포트'를 아래 구조에 맞춰 작성해줘.\n\n" +

                        "### [분석 데이터] ###\n" +
                        "1. 선수 맥락 (S3): %s\n" +
                        "2. 2026 예측 성적:\n" +
                        "   - 타율: %.3f (변화: %.3f)\n" +
                        "   - 홈런: %d개 (변화: %d)\n" + // 소수점 제거된 정수
                        "   - OPS: %.3f (변화: %.3f)\n\n" +

                        "### [리포트 작성 규칙] ###\n" +
                        "1. **항목별 분석**: 타율, 홈런, OPS 순서로 문단을 나누어 분석할 것. \n" +
                        "2. **데이터와 나이의 결합**: 각 지표의 변화 원인을 'age_2026' 수치와 연결해 스카우터 관점에서 설명할 것. (예: 노련미에 의한 타율 상승, 에이징 커브에 따른 장타력 감소 등)\n" +
                        "3. **정수 사용**: 홈런 개수는 반드시 내가 제공한 정수(%d개)로만 언급할 것. 소수점 사용 금지.\n" +
                        "4. **종합 평가**: 마지막 문단에는 선수의 팀 내 역할과 향후 커리어 방향성에 대한 수석 스카우터의 총평을 남길 것.\n" +
                        "5. **어조**: 전문가답고 신뢰감 있는 문체를 유지하며, 너무 늘어지지 않게 핵심 위주로 작성할 것.\n",
                pred.getPlayer().getName(),
                s3Context,
                pred.getPredAvg(), pred.getAvgDiff(),
                hrVal, hrDiff,
                pred.getPredOps(), pred.getOpsDiff(),
                hrVal // 규칙 3번용 데이터
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
        payload.put("max_tokens", 700);
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