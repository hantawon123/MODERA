package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.ImageSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageScheduleRepository extends JpaRepository<ImageSchedule, Integer> {

    Optional<ImageSchedule> findByScheduleIdAndDelYn(Integer scheduleId, String delYn);
}
