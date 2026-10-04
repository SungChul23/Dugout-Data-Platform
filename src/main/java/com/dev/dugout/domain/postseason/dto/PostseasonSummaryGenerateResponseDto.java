package com.dev.dugout.domain.postseason.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "시즌 종합 요약 생성 결과 DTO")
@Builder
@Getter
public class PostseasonSummaryGenerateResponseDto {

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "팀명", example = "삼성 라이온즈")
    private String teamName;

    @Schema(description = "시즌 연도", example = "2026")
    private Integer season;

    @Schema(description = "생성되어 저장된 요약 (생성 실패 시 null, 기존 데이터는 유지)")
    private String summary;
}
