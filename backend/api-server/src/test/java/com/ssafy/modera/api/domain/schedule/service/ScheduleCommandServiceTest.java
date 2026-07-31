package com.ssafy.modera.api.domain.schedule.service;

import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.library.entity.ImageSchedule;
import com.ssafy.modera.api.domain.library.entity.UserSchedule;
import com.ssafy.modera.api.domain.library.repository.ImageScheduleRepository;
import com.ssafy.modera.api.domain.library.repository.UserScheduleRepository;
import com.ssafy.modera.api.domain.schedule.entity.Schedule;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleQueryRepository;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleCommandServiceTest {

    @Mock UserScheduleRepository userScheduleRepository;
    @Mock ScheduleRepository scheduleRepository;
    @Mock ImageScheduleRepository imageScheduleRepository;
    @Mock ScheduleQueryRepository scheduleQueryRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @InjectMocks ScheduleCommandService scheduleCommandService;

    private UserSchedule userSchedule;
    private Schedule schedule;
    private ImageSchedule imageSchedule;

    @BeforeEach
    void setUp() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-29T00:00:00Z");
        userSchedule = UserSchedule.builder()
                .userId(1).scheduleId(301).updatedAt(createdAt).build();
        schedule = Schedule.builder()
                .title("팀 프로젝트 회의").updatedAt(createdAt).build();
        imageSchedule = ImageSchedule.builder()
                .imageId(1024).scheduleId(301).build();
    }

    @Test
    void deleteThrowsWhenScheduleIsMissingOrNotOwned() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleCommandService.delete(1, 301))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode().getCode()
                ).isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void deleteSoftDeletesAllRowsAndClearsImageFlagWhenNoOtherCalendaredSchedule() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(scheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(schedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(imageSchedule));
        when(scheduleQueryRepository.existsOtherActiveCalendaredSchedule(1, 1024, 301))
                .thenReturn(false);

        var response = scheduleCommandService.delete(1, 301);

        assertThat(response.deleted()).isTrue();
        assertThat(userSchedule.getDelYn()).isEqualTo("Y");
        assertThat(schedule.getDelYn()).isEqualTo("Y");
        assertThat(imageSchedule.getDelYn()).isEqualTo("Y");
        verify(scheduleQueryRepository).softDeleteView(eq(1), eq(301), any(OffsetDateTime.class));
        verify(imageQueryRepository).updateCalendared(1, 1024, false);
    }

    @Test
    void deleteKeepsImageFlagWhenAnotherCalendaredScheduleRemains() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(scheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(schedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(imageSchedule));
        when(scheduleQueryRepository.existsOtherActiveCalendaredSchedule(1, 1024, 301))
                .thenReturn(true);

        scheduleCommandService.delete(1, 301);

        verify(imageQueryRepository, never()).updateCalendared(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void deleteSkipsImageViewWhenImageRelationAlreadyDeleted() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(scheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(schedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.empty());

        var response = scheduleCommandService.delete(1, 301);

        assertThat(response.deleted()).isTrue();
        verify(scheduleQueryRepository, never())
                .existsOtherActiveCalendaredSchedule(anyInt(), anyInt(), anyInt());
        verify(imageQueryRepository, never()).updateCalendared(anyInt(), anyInt(), anyBoolean());
        verify(scheduleQueryRepository).softDeleteView(eq(1), eq(301), any(OffsetDateTime.class));
    }

    @Test
    void changeCalendarThrowsWhenScheduleIsMissingOrNotOwned() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleCommandService.changeCalendar(1, 301, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode().getCode()
                ).isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void calendarTrueMarksScheduleAndImageCalendared() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(imageSchedule));

        var response = scheduleCommandService.changeCalendar(1, 301, true);

        assertThat(response.calendared()).isTrue();
        assertThat(userSchedule.isCalendared()).isTrue();
        verify(scheduleQueryRepository)
                .updateViewCalendared(eq(1), eq(301), eq(true), any(OffsetDateTime.class));
        verify(imageQueryRepository).updateCalendared(1, 1024, true);
        verify(scheduleQueryRepository, never())
                .existsOtherActiveCalendaredSchedule(anyInt(), anyInt(), anyInt());
    }

    @Test
    void calendarFalseClearsImageFlagWhenNoOtherCalendaredSchedule() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(imageSchedule));
        when(scheduleQueryRepository.existsOtherActiveCalendaredSchedule(1, 1024, 301))
                .thenReturn(false);

        var response = scheduleCommandService.changeCalendar(1, 301, false);

        assertThat(response.calendared()).isFalse();
        assertThat(userSchedule.isCalendared()).isFalse();
        verify(scheduleQueryRepository)
                .updateViewCalendared(eq(1), eq(301), eq(false), any(OffsetDateTime.class));
        verify(imageQueryRepository).updateCalendared(1, 1024, false);
    }

    @Test
    void calendarFalseKeepsImageFlagWhenAnotherCalendaredScheduleRemains() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.of(imageSchedule));
        when(scheduleQueryRepository.existsOtherActiveCalendaredSchedule(1, 1024, 301))
                .thenReturn(true);

        scheduleCommandService.changeCalendar(1, 301, false);

        verify(imageQueryRepository).updateCalendared(1, 1024, true);
    }

    @Test
    void calendarSkipsImageViewWhenImageRelationAlreadyDeleted() {
        when(userScheduleRepository.findByUserIdAndScheduleIdAndDelYn(1, 301, "N"))
                .thenReturn(Optional.of(userSchedule));
        when(imageScheduleRepository.findByScheduleIdAndDelYn(301, "N"))
                .thenReturn(Optional.empty());

        var response = scheduleCommandService.changeCalendar(1, 301, true);

        assertThat(response.calendared()).isTrue();
        assertThat(userSchedule.isCalendared()).isTrue();
        verify(scheduleQueryRepository)
                .updateViewCalendared(eq(1), eq(301), eq(true), any(OffsetDateTime.class));
        verify(imageQueryRepository, never()).updateCalendared(anyInt(), anyInt(), anyBoolean());
    }
}
