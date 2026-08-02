package com.ssafy.modera.api.domain.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.library.repository.ImageScheduleRepository;
import com.ssafy.modera.api.domain.library.repository.UserScheduleRepository;
import com.ssafy.modera.api.domain.schedule.entity.Schedule;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleQueryRepository;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleCreationServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock UserScheduleRepository userScheduleRepository;
    @Mock ImageScheduleRepository imageScheduleRepository;
    @Mock ScheduleQueryRepository scheduleQueryRepository;
    @Mock UserDataChangeOutboxService userDataChangeOutboxService;

    private ScheduleCreationService scheduleCreationService;

    @BeforeEach
    void setUp() {
        scheduleCreationService = new ScheduleCreationService(
                scheduleRepository,
                userScheduleRepository,
                imageScheduleRepository,
                scheduleQueryRepository,
                new ObjectMapper(),
                userDataChangeOutboxService
        );
    }

    @Test
    void createsCandidateInterpretingKoreanLocalTime() {
        when(scheduleQueryRepository.existsActiveScheduleForImage(3, 51)).thenReturn(false);
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String fields = """
                {"startYear":null,"startMonth":null,"startDay":null,"startTime":null,
                 "endYear":2026,"endMonth":8,"endDay":28,"endTime":"18:00"}
                """;

        scheduleCreationService.createFromAnalysis(3, 51, "AI 공모전", "schedule", fields);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        Schedule saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("AI 공모전");
        assertThat(saved.getStartAt()).isNull();
        assertThat(saved.getEndAt().toInstant()).isEqualTo(
                OffsetDateTime.of(2026, 8, 28, 18, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant());
        verify(userScheduleRepository).save(any());
        verify(imageScheduleRepository).save(any());
        verify(scheduleQueryRepository).insertView(
                eq(3), any(), eq(51), eq("AI 공모전"),
                isNull(), eq(saved.getEndAt()), any(OffsetDateTime.class));
    }

    @Test
    void ignoresNonScheduleStructuredType() {
        scheduleCreationService.createFromAnalysis(3, 51, "상품", "product-info", "{}");

        verifyNoInteractions(
                scheduleRepository, userScheduleRepository,
                imageScheduleRepository, scheduleQueryRepository);
    }

    @Test
    void skipsWhenActiveScheduleAlreadyExists() {
        when(scheduleQueryRepository.existsActiveScheduleForImage(3, 51)).thenReturn(true);

        scheduleCreationService.createFromAnalysis(3, 51, "제목", "schedule", "{}");

        verify(scheduleRepository, never()).save(any());
        verify(scheduleQueryRepository, never())
                .insertView(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsDatelessCandidateWhenFieldsAreBroken() {
        when(scheduleQueryRepository.existsActiveScheduleForImage(3, 51)).thenReturn(false);
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleCreationService.createFromAnalysis(3, 51, "제목", "schedule", "not-json{{");

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStartAt()).isNull();
        assertThat(captor.getValue().getEndAt()).isNull();
        verify(scheduleQueryRepository).insertView(
                eq(3), any(), eq(51), eq("제목"), isNull(), isNull(), any(OffsetDateTime.class));
    }

    @Test
    void parsesNumericStringsAndDropsEndBeforeStart() {
        when(scheduleQueryRepository.existsActiveScheduleForImage(3, 51)).thenReturn(false);
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String fields = """
                {"startYear":"2026","startMonth":"8","startDay":"28","startTime":"18:00",
                 "endYear":2026,"endMonth":8,"endDay":27,"endTime":"18:00"}
                """;

        scheduleCreationService.createFromAnalysis(3, 51, "제목", "schedule", fields);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStartAt().toInstant()).isEqualTo(
                OffsetDateTime.of(2026, 8, 28, 18, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant());
        assertThat(captor.getValue().getEndAt()).isNull();
    }
}
