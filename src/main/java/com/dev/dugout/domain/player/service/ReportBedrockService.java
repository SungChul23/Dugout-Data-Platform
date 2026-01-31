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
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터입니다. "
                        + "아래 지침에 따라 %s 선수의 2026 시즌 타자 분석 리포트를 작성하십시오.\n\n" +

                        "[2026 시즌 분석 리포트 - %s 선수]\n\n" +

                        "본 리포트는 단순한 성적 전망 요약이 아니라, 제공된 수치와 맥락 정보를 바탕으로 "
                        + "해당 선수가 2026 시즌 팀의 핵심 타격 자원으로 평가되기에 충분한지 여부를 "
                        + "스카우터의 관점에서 판단하고 검토하는 것을 목표로 합니다.\n\n" +

                        "분석에 사용할 수 있는 정보는 다음과 같습니다.\n" +
                        "선수 맥락 정보(S3): %s\n" +
                        "2026 시즌 성적 전망 수치: "
                        + "타율 %.3f(변화 %.3f), 홈런 %d개(변화 %d), OPS %.3f(변화 %.3f)\n\n" +

                        "리포트 작성 규칙은 다음과 같습니다.\n" +
                        "모든 문장은 '~합니다', '~입니다'와 같은 정중한 경어체로 작성하십시오.\n" +
                        "타이틀 이후에는 별도의 소제목 없이 바로 본문 분석을 작성하십시오.\n" +
                        "마크다운 기호나 특수 기호는 사용하지 말고 텍스트와 줄바꿈만으로 구성하십시오.\n" +
                        "분량은 총 3문단으로 제한하되, 각 문단은 서로 다른 분석 목적을 가지도록 구성하십시오.\n\n" +

                        "분석 구성은 다음 흐름을 반드시 따르십시오.\n" +
                        "첫 번째 문단에서는 타율과 OPS 변화를 중심으로 "
                        + "타격 생산성의 전반적인 흐름과 현재 수치가 가지는 경쟁력을 객관적으로 평가하십시오.\n" +
                        "두 번째 문단에서는 2026 시즌 나이(age_2026)를 고려하여 에이징 커브 관점에서 "
                        + "장타력 변화와 홈런 %d개의 현실성, 그리고 성적 유지 가능성을 분석하십시오.\n" +
                        "세 번째 문단에서는 앞선 분석을 종합하여 이 선수가 2026 시즌 "
                        + "확정적인 중심 타선 자원인지, 혹은 조건부 핵심 타자로 분류하는 것이 타당한지를 "
                        + "팀 내 역할과 함께 명확히 정리하십시오.\n",
                playerName,
                playerName,
                s3Context,
                pred.getPredAvg(), pred.getAvgDiff(),
                hrVal, hrDiff,
                pred.getPredOps(), pred.getOpsDiff(),
                hrVal
        );

    }

    // [투수용 프롬프트 생성]
    private String constructPitcherPrompt(PredictionResult pred, String s3Context) {
        String playerName = pred.getPlayer().getName();
        double eraProb = pred.getEraEliteProb() != null ? pred.getEraEliteProb().doubleValue() * 100 : 0;
        double whipProb = pred.getWhipEliteProb() != null ? pred.getWhipEliteProb().doubleValue() * 100 : 0;

        return String.format(
                "너는 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터입니다. "
                        + "아래 지침에 따라 %s 선수의 2026 시즌 투수 분석 리포트를 작성하십시오.\n\n" +

                        "[2026 시즌 분석 리포트 - %s 선수]\n\n" +

                        "본 리포트는 단순한 성적 전망 요약이 아니라, 제공된 성적 수치와 선수 맥락을 바탕으로 "
                        + "해당 선수가 2026 시즌 팀 마운드의 핵심 전력으로 평가되기에 충분한지 여부를 "
                        + "스카우터의 관점에서 판단하고 검토하는 것을 목표로 합니다.\n\n" +

                        "분석에 사용할 수 있는 정보는 다음과 같습니다.\n" +
                        "선수 맥락 정보(S3): %s\n" +
                        "2026 시즌 성적 전망 수치: ERA %.2f, WHIP %.2f\n\n" +

                        "리포트 작성 규칙은 다음과 같습니다.\n" +
                        "모든 문장은 '~합니다', '~입니다'와 같은 정중한 경어체로 작성하십시오.\n" +
                        "타이틀 이후에는 별도의 소제목 없이 바로 본문 분석을 작성하십시오.\n" +
                        "마크다운 기호나 특수 기호는 사용하지 말고 텍스트와 줄바꿈만으로 구성하십시오.\n" +
                        "분량은 총 3문단으로 제한하되, 각 문단은 서로 다른 분석 목적을 가지도록 구성하십시오.\n\n" +

                        "분석 구성은 다음 흐름을 반드시 따르십시오.\n" +
                        "첫 번째 문단에서는 ERA와 WHIP를 중심으로 "
                        + "이 투수가 보여주는 전반적인 경기 지배력과 안정성을 객관적으로 평가하십시오.\n" +
                        "두 번째 문단에서는 2026 시즌 나이(age_2026)를 고려하여 "
                        + "에이징 커브 관점에서 구위 유지 가능성, 이닝 소화 능력, 성적 변동 리스크를 분석하십시오.\n" +
                        "세 번째 문단에서는 앞선 분석을 종합하여 이 선수가 2026 시즌 "
                        + "확정적인 선발 로테이션 핵심 자원인지, "
                        + "혹은 조건부 전력으로 분류하는 것이 타당한지를 "
                        + "팀 마운드 운용 관점에서 명확히 정리하십시오.\n",
                playerName,
                playerName,
                s3Context,
                pred.getPredEra(), pred.getPredWhip()
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