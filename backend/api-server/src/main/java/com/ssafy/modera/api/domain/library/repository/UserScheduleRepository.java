package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Integer> {

    Optional<UserSchedule> findByUserIdAndScheduleIdAndDelYn(Integer userId, Integer scheduleId, String delYn);
}
