package com.dev.dugout.domain.player.dto;

//JSON 필드 이름과 자바 필드 이름을 매핑해주는 어노테이션
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// "선수 미래성적 예측 전용 응답 DTO"
public class PlayerResponseDto {
    private Long playerId; //여기서 사용하는 playerId는 kbo_pcode
    private String name;

    @JsonProperty("back_number") // 프론트엔드 ServerPlayerDto 인터페이스 일치
    private Integer backNumber;

    @JsonProperty("position_type") // 프론트엔드 ServerPlayerDto 인터페이스 일치
    private String positionType;
}
