package com.dev.dugout.domain.admin.repository;

import com.dev.dugout.domain.admin.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    //ID 역순으로 가져오기 (최신순) - 전체조회
    List<Notice> findAllByOrderByIdDesc();
}
