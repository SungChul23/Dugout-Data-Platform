package com.dev.dugout.domain.team.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
//구단별 티켓 예메 사이트 제공 DB로 이전 (예매처 수정 대비)
public class TeamTicketResponseDto {

    private Long id;
    private String name;        // 팀 이름
    private String city;        // 연고지 (대전, 서울 등)
    private String stadiumName; // 홈구장 이름
    private String bookingUrl;  // 예매 링크

}
