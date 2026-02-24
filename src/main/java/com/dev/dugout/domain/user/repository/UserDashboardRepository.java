package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.entity.UserDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserDashboardRepository extends JpaRepository<UserDashboard, Long> {

    //대시보드 진입 시 1, 2, 3번 슬롯에 데이터가 있는지 확인하는 용도
    List<UserDashboard> findByUser(User user);

    // 특정 유저의 특정 슬롯 삭제 (교체 및 삭제용)
    void deleteByUserAndSlotNumber(User user, Integer slotNumber);
}