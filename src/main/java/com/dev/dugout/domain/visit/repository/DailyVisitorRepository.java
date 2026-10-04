package com.dev.dugout.domain.visit.repository;

import com.dev.dugout.domain.visit.entity.DailyVisitor;
import com.dev.dugout.domain.visit.entity.DailyVisitorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DailyVisitorRepository extends JpaRepository<DailyVisitor, DailyVisitorId> {

    // 같은 날 같은 방문자는 PK 중복으로 무시된다 (동시 요청에도 DB에서 원자적으로 처리)
    @Modifying
    @Query(value = "INSERT IGNORE INTO daily_visitor (visit_date, visitor_key) VALUES (:visitDate, :visitorKey)",
            nativeQuery = true)
    void insertIgnore(@Param("visitDate") LocalDate visitDate, @Param("visitorKey") String visitorKey);

    long countByVisitDate(LocalDate visitDate);
}
