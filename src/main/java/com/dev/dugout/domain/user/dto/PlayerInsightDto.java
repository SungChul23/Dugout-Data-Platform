package com.dev.dugout.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "대시보드 응원 선수 예측 인사이트 DTO")
@Data
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerInsightDto {

    @Schema(description = "슬롯 번호 (1~3)", example = "1")
    private Integer slotNumber;

    @Schema(description = "선수 고유 ID", example = "79047")
    private Long playerId;

    @Schema(description = "선수명", example = "김도영")
    private String name;

    @Schema(description = "등번호", example = "5")
    private Integer backNumber;

    @Schema(description = "포지션", example = "3B")
    private String position;

    @Schema(description = "팀 코드", example = "KIA")
    private String teamCode;

    @Schema(description = "슬롯이 비어 있는지 여부", example = "false")
    @JsonProperty("isEmpty")
    private boolean isEmpty;

    @Schema(description = "[타자] 예측 타율", example = "0.341")
    private BigDecimal predictedAvg;

    @Schema(description = "[타자] 예측 OPS", example = "1.001")
    private BigDecimal predictedOps;

    @Schema(description = "[타자] 예측 홈런 수", example = "35")
    private Integer predictedHr;

    @Schema(description = "[타자] 타율 변화량", example = "-0.017")
    private BigDecimal avgDiff;

    @Schema(description = "[타자] OPS 변화량", example = "-0.042")
    private BigDecimal opsDiff;

    @Schema(description = "[타자] 홈런 변화량", example = "11")
    private Integer hrDiff;

    @Schema(description = "[투수] 엘리트 등극 확률 (0~1)", example = "0.823")
    private BigDecimal probElite;

    @Schema(description = "[투수] 보직 내 상위 백분율", example = "12.5")
    private BigDecimal rolePercentileTop;

    @Schema(description = "[투수] 보직 내 순위", example = "3")
    private Integer roleRank;

    @Schema(description = "[투수] 보직 내 전체 인원", example = "24")
    private Integer roleTotal;
}
