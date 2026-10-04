package com.dev.dugout.domain.visit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

//일자별 순 방문자 (같은 IP는 하루에 한 행만 저장)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(DailyVisitorId.class)
@Table(name = "daily_visitor")
public class DailyVisitor {

    @Id
    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Id
    @Column(name = "visitor_key", columnDefinition = "CHAR(64)")
    private String visitorKey; // 원본 IP 대신 저장하는 SHA-256 해시
}
