package com.dev.dugout.domain.admin.repository;

import com.dev.dugout.domain.admin.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    // 날짜 문자열 기준 내림차순 (최신 날짜가 위로)
    List<Notice> findAllByOrderByUpdateDateDesc();
}
