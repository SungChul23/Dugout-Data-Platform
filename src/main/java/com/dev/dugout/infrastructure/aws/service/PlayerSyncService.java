package com.dev.dugout.infrastructure.aws.service;

import com.dev.dugout.domain.player.entity.Player;
import com.dev.dugout.domain.player.repository.PlayerRepository;
import com.dev.dugout.domain.team.entity.Team;
import com.dev.dugout.domain.team.repository.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerSyncService {

    private final LambdaClient lambdaClient; // AwsLambdaConfig에서 등록한 Bean
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final ObjectMapper objectMapper;

    // JPA의 영속성 컨텍스트를 직접 제어
    private final EntityManager entityManager;

    //누락된 PCODE 리스트를 받아 람다를 통해 신규 선수를 DB에 등록
    @Transactional
    public void syncMissingPlayers(Set<String> missingPcodes) {
        if (missingPcodes == null || missingPcodes.isEmpty()) {
            return;
        }

        try {
            log.info(">>>> [신규 선수 동기화 시작] 대상 PCODE 리스트: {}", missingPcodes);

            // 1. 람다 호출 페이로드(JSON) 생성
            String jsonPayload = objectMapper.writeValueAsString(Map.of("pcodes", missingPcodes));

            // 2. 람다 호출 요청 (SDK v2 방식)
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName("dugout-missing-player-scraper") // 람다 함수명
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build();

            // 3. 람다 실행 및 응답 수신 (동기 방식 - 트리거)
            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            String responseJson = response.payload().asUtf8String();

            // 4. 응답 데이터 파싱
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode playersNode = root.path("players");

            if (playersNode.isArray() && !playersNode.isEmpty()) {
                for (JsonNode p : playersNode) {
                    String pcode = p.get("kboPcode").asText();

                    // 중복 체크 (방어 로직)
                    if (playerRepository.existsByKboPcode(pcode)) {
                        continue;
                    }

                    // 람다가 준 teamId (1~10)로 우리 DB의 Team 엔티티 조회
                    Long teamId = p.get("teamId").asLong();
                    Team team = teamRepository.findById(teamId)
                            .orElseThrow(() -> new EntityNotFoundException("해당 팀 ID를 찾을 수 없습니다: " + teamId));

                    // 5. 신규 Player 엔티티 생성 및 저장
                    Player newPlayer = Player.builder()
                            .kboPcode(pcode)
                            .name(p.get("name").asText())
                            .backNumber(p.get("backNumber").asInt())
                            .birth(p.get("birth").asText())
                            .positionType(p.get("positionType").asText())
                            .subPositionType(null) // 요청대로 NULL 처리
                            .team(team)             // 연관 관계 매핑 (예: 한화 6번)
                            .isPredictable(false)   // 고정값
                            .label("일반")           // 고정값
                            .build();

                    playerRepository.save(newPlayer);
                    log.info(">>>>  [자동 등록 완료] {} (PCODE: {}, 팀: {})",
                            newPlayer.getName(), pcode, team.getName());
                }
                //[핵심 추가] 영속성 컨텍스트 동기화
                // 1. 현재까지 save한 내용(INSERT 문)을 DB로 즉시 보냄
                entityManager.flush();

                // 2. 1차 캐시를 비웁니다.
                // 이렇게 해야 다음 단계(성적 적재)에서 DB를 다시 찔러 신규 선수를 인식
                entityManager.clear();
                log.info(">>>> [영속성 컨텍스트 초기화] 이제 다음 공정에서 신규 선수를 인식할 수 있습니다.");

            } else {
                log.warn(">>>> 람다로부터 선수 정보를 받지 못했습니다. (PCODE: {})", missingPcodes);
            }

        } catch (Exception e) {
            log.error(">>>>  [동기화 실패] 신규 선수 등록 중 오류 발생: {}", e.getMessage());
            // 예외를 던져서 상위 트랜잭션(적재 공정)도 함께 롤백되도록 함
            throw new RuntimeException("신규 선수 동기화 실패", e);
        }
    }
}
