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
    @Column(name = "player_id")
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    private String positionType;    // 내야수, 외야수, 투수 등
    private String subPositionType; // null 허용 (추후 고도화용)
    private Integer backNumber;

//    @Column(nullable = false)
//    @Builder.Default
//    private Boolean isRisingStar = false; // 대시보드 노출용 라이징 스타


    // 많은 선수들 중 성적 예측이 가능한 선수 필터링
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPredictable = false;

    //s3에있는 선수의 성적과 매핑
    private String s3_path;

}
