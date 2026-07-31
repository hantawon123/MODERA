package com.ssafy.modera.api.domain.schedule.repository;

import com.ssafy.modera.api.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    Optional<Schedule> findByScheduleIdAndDelYn(Integer scheduleId, String delYn);
}
