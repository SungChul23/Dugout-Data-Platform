package com.dev.dugout.domain.team.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

//TeamSeasonSummary 엔티티의 복합키를 정의하는 클래스
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode //JPA가 식별자를 비교할 때 사용
public class TeamSeasonSummaryId implements Serializable {

    private Long teamId;
    private Long season;

}
