package com.dev.dugout.domain.player.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity
@Table(name = "fa_market")
@Getter
@Setter
@IdClass(FaMarketId.class) // pocde + 년도 = 복합키 사용
public class FaMarket {

    @Id
    @Column(length = 20)
    private String pcode;

    @Id
    private Integer year;

    @Column(name = "player_name", length = 50)
    private String playerName;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(length = 1)
    private String grade;

    @Column(name = "position_type", length = 10)
    private String positionType;

    @Column(name = "sub_position_type", length = 10)
    private String subPositionType;

    private Integer age;


    // 투수 지표
    @Column(name = "stat_pitching", precision = 5, scale = 2)
    private BigDecimal statPitching;

    @Column(name = "stat_stability", precision = 5, scale = 2)
    private BigDecimal statStability;

    // 타자 지표
    @Column(name = "stat_offense", precision = 5, scale = 2)
    private BigDecimal statOffense;

    @Column(name = "stat_defense", precision = 5, scale = 2)
    private BigDecimal statDefense;

    // 공통 지표
    @Column(name = "stat_contribution", precision = 5, scale = 2) // 기여점수
    private BigDecimal statContribution;

    @Column(name = "is_fa_target") // fa 대상인지
    private Boolean isFaTarget = false;

    @Column(name = "ai_feedback", columnDefinition = "TEXT") // 베드락 피드백
    private String aiFeedback;

    @Column(name = "player_intro", length = 255) // 선수 한줄 소개
    private String playerIntro;

    @Column(name = "current_salary") // 현재 연봉
    private Long currentSalary;
}
