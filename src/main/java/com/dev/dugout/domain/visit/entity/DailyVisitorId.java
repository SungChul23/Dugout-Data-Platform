package com.dev.dugout.domain.visit.entity;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

//DailyVisitor 엔티티의 복합키를 정의하는 클래스
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode //JPA가 식별자를 비교할 때 사용
public class DailyVisitorId implements Serializable {

    private LocalDate visitDate;
    private String visitorKey;

}
