package com.dev.dugout.domain.visit.service;

import com.dev.dugout.domain.visit.dto.VisitCountResponseDto;
import com.dev.dugout.domain.visit.repository.DailyVisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class VisitService {

    // 서버 시간대와 무관하게 한국 시간 자정에 날짜가 바뀌도록 고정
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyVisitorRepository dailyVisitorRepository;

    @Transactional
    public VisitCountResponseDto recordVisit(String clientIp) {
        LocalDate today = LocalDate.now(KST);
        dailyVisitorRepository.insertIgnore(today, toVisitorKey(today, clientIp));
        return toResponse(today);
    }

    @Transactional(readOnly = true)
    public VisitCountResponseDto getTodayCount() {
        return toResponse(LocalDate.now(KST));
    }

    private VisitCountResponseDto toResponse(LocalDate date) {
        return VisitCountResponseDto.builder()
                .date(date)
                .count(dailyVisitorRepository.countByVisitDate(date))
                .build();
    }

    // 원본 IP는 저장하지 않는다. 날짜를 함께 해시해 같은 IP라도 날마다 다른 키가 된다
    private String toVisitorKey(LocalDate date, String clientIp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((date + "|" + clientIp).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
