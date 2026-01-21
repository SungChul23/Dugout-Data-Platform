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
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터야. " +
                        "제공된 S3 데이터와 DB의 예측 수치를 종합해서 %s 선수의 '2026 시즌 분석 리포트'를 작성해줘.\n\n" +

                        "### [분석 대상 데이터] ###\n" +
                        "1. 선수 기본 정보 및 나이 (S3): %s\n" +
                        "2. 2026 예측 성적 (DB):\n" +
                        "   - 타율: %.3f (전년 대비 변화: %.3f)\n" +
                        "   - 홈런: %d개 (전년 대비 변화: %d)\n" +
                        "   - OPS: %.3f (전년 대비 변화: %.3f)\n\n" +

                        "### [리포트 작성 지침] ###\n" +
                        "1. **수치 중심 분석**: 예측된 타율, 홈런, OPS의 변화량을 기반으로 2026년의 전반적인 타격 생산성을 먼저 진단할 것.\n" +
                        "2. **나이를 통한 근거 제시**: 성적 변화(diff)의 원인을 설명할 때 'age_2026' 수치를 활용할 것. " +
                        "(예: 성적이 오른다면 '전성기 연령 진입에 따른 기량 만개', 하락한다면 '에이징 커브에 따른 신체 능력 조정' 등)\n" +
                        "3. **스카우터의 시각**: 단순히 숫자를 읽어주지 말고, 이 수치가 이 선수의 커리어에서 어떤 의미(예: 장타자로의 변신, 정교함의 완성 등)를 갖는지 전문가답게 평가할 것.\n" +
                        "4. **어조 및 분량**: '더그아웃' 서비스의 권위 있는 전문가 톤으로 자연스럽게 작성하며, 문장 수에 구애받지 말고 핵심 내용을 충분히 전달할 것. (한국어 작성)\n",
                pred.getPlayer().getName(),
                s3Context,
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
        payload.put("max_tokens", 900);
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