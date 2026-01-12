package com.dev.dugout.domain.player.entity;


import com.dev.dugout.domain.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; //team 테이블의 팀 코드 참조

    @Column(nullable = false)
    private String name;

    private String positionType;    // 내야수, 외야수, 투수 등
    private String subPositionType; // 1루수, 유격수 등 (추후 골든 글러브 예측 떄 사용 예정)
    private Integer backNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRisingStar = false; // 대시보드 노출용

}
