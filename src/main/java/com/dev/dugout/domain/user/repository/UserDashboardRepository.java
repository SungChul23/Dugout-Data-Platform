package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.entity.UserDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserDashboardRepository extends JpaRepository<UserDashboard, Long> {


    //특정 유저가 대시보드에 등록한 모든 선수(슬롯 정보 포함)를 조회
    //대시보드 진입 시 1, 2, 3번 슬롯에 데이터가 있는지 확인하는 용도
    List<UserDashboard> findByUser(User user);

    //특정 유저가 특정 슬롯(1, 2, 3)에 등록한 선수가 있는지 확인
    boolean existsByUserAndSlotNumber(User user, Integer slotNumber);
}