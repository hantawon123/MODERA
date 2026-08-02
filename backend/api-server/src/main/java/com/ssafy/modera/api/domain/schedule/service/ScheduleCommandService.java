package com.ssafy.modera.api.domain.schedule.service;

import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.library.entity.ImageSchedule;
import com.ssafy.modera.api.domain.library.entity.UserSchedule;
import com.ssafy.modera.api.domain.library.repository.ImageScheduleRepository;
import com.ssafy.modera.api.domain.library.repository.UserScheduleRepository;
import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleCalendarResponse;
import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleDeleteResponse;
import com.ssafy.modera.api.domain.schedule.exception.ScheduleErrorCode;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleQueryRepository;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ScheduleCommandService {

    private final UserScheduleRepository userScheduleRepository;
    private final ScheduleRepository scheduleRepository;
    private final ImageScheduleRepository imageScheduleRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final UserDataChangeOutboxService userDataChangeOutboxService;

    @Transactional
    public ScheduleDeleteResponse delete(Integer userId, Integer scheduleId) {
        UserSchedule userSchedule = findActiveUserSchedule(userId, scheduleId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 출처 이미지 관계가 이미 삭제된 일정(5-3 이미지 삭제 이후)은 user_image_view 갱신을 생략한다.
        ImageSchedule imageSchedule = imageScheduleRepository
                .findByScheduleIdAndDelYn(scheduleId, "N")
                .orElse(null);
        // JPA 변경이 flush되기 전에 SQL로 읽으므로, 현재 일정을 제외한 조건으로 미리 계산한다.
        boolean otherCalendared = imageSchedule != null
                && scheduleQueryRepository.existsOtherActiveCalendaredSchedule(
                        userId, imageSchedule.getImageId(), scheduleId);

        scheduleRepository.findByScheduleIdAndDelYn(scheduleId, "N")
                .ifPresent(schedule -> schedule.softDelete(now));
        userSchedule.softDelete(now);
        if (imageSchedule != null) {
            imageSchedule.softDelete();
        }
        scheduleQueryRepository.softDeleteView(userId, scheduleId, now);
        if (imageSchedule != null && !otherCalendared) {
            imageQueryRepository.updateCalendared(userId, imageSchedule.getImageId(), false);
        }
        userDataChangeOutboxService.record(
                userId, UserDataChangeResource.SCHEDULE, String.valueOf(scheduleId));

        return new ScheduleDeleteResponse(scheduleId, true);
    }

    @Transactional
    public ScheduleCalendarResponse changeCalendar(Integer userId, Integer scheduleId, boolean calendared) {
        UserSchedule userSchedule = findActiveUserSchedule(userId, scheduleId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ImageSchedule imageSchedule = imageScheduleRepository
                .findByScheduleIdAndDelYn(scheduleId, "N")
                .orElse(null);
        Boolean imageCalendared = null;
        if (imageSchedule != null) {
            imageCalendared = calendared || scheduleQueryRepository.existsOtherActiveCalendaredSchedule(
                    userId, imageSchedule.getImageId(), scheduleId);
        }

        userSchedule.changeCalendared(calendared, now);
        scheduleQueryRepository.updateViewCalendared(userId, scheduleId, calendared, now);
        if (imageCalendared != null) {
            imageQueryRepository.updateCalendared(userId, imageSchedule.getImageId(), imageCalendared);
        }
        userDataChangeOutboxService.record(
                userId, UserDataChangeResource.SCHEDULE, String.valueOf(scheduleId));

        return new ScheduleCalendarResponse(scheduleId, calendared, now);
    }

    private UserSchedule findActiveUserSchedule(Integer userId, Integer scheduleId) {
        return userScheduleRepository
                .findByUserIdAndScheduleIdAndDelYn(userId, scheduleId, "N")
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
    }
}
