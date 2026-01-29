package com.dev.dugout.domain.player.service;


import com.dev.dugout.domain.player.entity.FaMarket;
import com.dev.dugout.infrastructure.aws.s3.S3Service;
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
public class FaMarketBedrockService {
    private final S3Service s3Service;
    private final BedrockRuntimeClient bedrockClient;


    // FA 전용 마스터 데이터 캐시 (pcode 기준)
    private final Map<String, String> faMasterDataMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("====> [FA 초기화] S3 FA 마스터 데이터를 메모리에 로드합니다.");
        // 성철님이 S3Service에 새로 만든 FA 전용 메서드 호출
        loadFaMasterData(s3Service.fetchFaBatterMasterJson(), "FA 타자");
        loadFaMasterData(s3Service.fetchFaPitcherMasterJson(), "FA 투수");
    }

    private void loadFaMasterData(String jsonContent, String type) {
        if (jsonContent != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    // JSON에서 "pcode"라는 키를 찾아 추출 -> 곧 선수 추출
                    String pcode = String.valueOf(obj.get("pcode"));
                    // 이 pcode를 Key로, 해당 선수의 전체 정보(JSON String)를 Value로 메모리(Map)에 저장
                    faMasterDataMap.put(pcode, obj.toString());
                }
                log.info("====> [FA 성공] {} 명의 {} 데이터를 캐싱했습니다.", jsonArray.length(), type);
            } catch (Exception e) {
                log.error("#### [FA 초기화 실패] {} JSON 파싱 오류: {}", type, e.getMessage());
            }
        }
    }

    public String generateFaReport(FaMarket player) {
        // DB에서 가져온 현재 선수 객체(player)의 pcode를 가져옴
        String pcode = player.getPcode();
        // 메모리에 캐싱된 Map에서 해당 pcode를 가진 선수의 과거 데이터를 쏙 뽑아오자
        String masterContext = faMasterDataMap.getOrDefault(pcode, "유사 과거 사례 정보 없음");

        String prompt = constructFaPrompt(player, masterContext);
        return invokeBedrock(prompt);
    }

    private String constructFaPrompt(FaMarket player, String masterContext) {
        boolean isPitcher = "투수".equals(player.getPositionType());
        String playerName = player.getPlayerName();

        String statsInfo = isPitcher
                ? String.format("구위: %s, 안정성: %s, 기여도: %s", player.getStatPitching(), player.getStatStability(), player.getStatContribution())
                : String.format("공격: %s, 수비: %s, 기여도: %s", player.getStatOffense(), player.getStatDefense(), player.getStatContribution());

        return String.format(
                "당신은 이제부터 야구 데이터 분석 전문 '더그아웃'의 수석 스카우터입니다. 이번 FA 시장에 나온 %s 선수의 시장 가치 분석 리포트를 정중하게 작성해주세요.\n\n" +

                        "### [분석 데이터] ###\n" +
                        "1. 과거 유사 사례 데이터 (S3): %s\n" +
                        "2. 현재 선수의 FA 지표 점수 (100점 만점 기준): %s\n" +
                        "3. 선수 기본 정보: %d세, %s 등급, %s\n\n" +

                        "### [리포트 작성 규칙] ###\n" +
                        "1. 모든 문장은 '~합니다', '~입니다'와 같은 정중한 경어체를 사용하여 신뢰감 있는 스카우팅 리포트를 작성하십시오.\n" +
                        "2. 별도의 제목이나 머리말 없이 바로 본문 분석 내용을 작성하십시오.\n" +
                        "3. '#', '*', '-' 등 마크다운 서식 기호를 절대 사용하지 말고, 오직 텍스트와 줄바꿈으로만 내용을 구성하십시오.\n" +
                        "4. 과거 유사 지표를 가진 선수들의 계약 사례와 현재 데이터를 면밀히 대조하여, 이 선수가 시장에서 받을 대우와 등급(%s) 선정의 적절성을 논리적으로 분석하십시오.\n" +
                        "5. '에이징 커브'라는 용어는 분석상 반드시 필요한 경우에만 한정하여 사용하고, 무분별한 남발을 피하며 선수의 기량 지속 가능성에 초점을 맞추십시오.\n" +
                        "6. 분량은 3문단에서 정도로 상세하고 깊이 있게 작성하십시오.\n",
                playerName, masterContext, statsInfo,
                player.getAge(), player.getGrade(), player.getSubPositionType(),
                player.getGrade()
        );
    }

    private String invokeBedrock(String prompt) {
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

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error(">>>> [FA BEDROCK ERROR] 리포트 생성 실패: {}", e.getMessage());
            return "현재 해당 선수의 FA 시장 가치를 분석 중입니다. 잠시 후 확인해 주세요.";
        }
    }
}
