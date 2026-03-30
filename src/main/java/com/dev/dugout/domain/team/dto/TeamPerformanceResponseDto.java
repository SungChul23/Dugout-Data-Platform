package com.dev.dugout.domain.team.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;


@Getter
@Builder
public class TeamPerformanceResponseDto {
    private Long teamId;
    private String teamName;
    private LocalDate baseDate; //최신화 기준

    // 1. 팀 타자 지표 (12개)
    private BigDecimal avg;    // avgh2
    private Integer hr;        // hrh1
    private Integer runs;      // rh1
    private Integer hits;      // hh1
    private Integer rbi;       // rbih1
    private BigDecimal obp;    // obph2
    private BigDecimal ops;    // opsh2
    private BigDecimal risp;   // risph2
    private BigDecimal slg;    // slgh2
    private BigDecimal phBa;   // phbah2
    private Integer multiHit;  // mhh2
    private Integer totalBases; // tbh1

    // 2. 팀 투수 지표 (12개)
    private BigDecimal era;    // erap1
    private Integer wins;      // wp1
    private Integer so;        // sop1
    private Integer sv;        // svp1
    private Integer hld;       // hldp1
    private BigDecimal wpct;   // wpctp1
    private BigDecimal whip;   // whipp1
    private Integer qs;        // qsp2
    private BigDecimal oppAvg; // avgp2 (피안타율)
    private Integer bsv;       // bsvp2
    private Integer np;        // npp2
    private Integer hrAllowed; // hrp1

    // 3. 팀 수비/주루 지표 (12개)
    private Integer sb;        // sbr (도루성공)
    private BigDecimal sbRate; // sbrater
    private Integer error;     // ed
    private BigDecimal fpct;   // fpctd
    private Integer dp;        // dpd
    private BigDecimal csRate; // csrated
    private Integer oob;       // oobr
    private Integer sba;       // sbar
    private Integer pkoR;      // pkor
    private Integer pkoD;      // pkod
    private Integer cs;        // csd
    private Integer sbAllowed; // sbd
}