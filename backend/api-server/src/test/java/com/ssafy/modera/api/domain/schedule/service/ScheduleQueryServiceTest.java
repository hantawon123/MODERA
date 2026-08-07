package com.ssafy.modera.api.domain.schedule.service;

import com.ssafy.modera.api.domain.schedule.repository.ScheduleListPage;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleListRow;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleQueryServiceTest {

    @Mock ScheduleQueryRepository scheduleQueryRepository;
    @InjectMocks ScheduleQueryService scheduleQueryService;

    @Test
    void rejectsUnsupportedSort() {
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, null, null, 0, 20, "UPLOADED_DESC"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsOutOfRangePageAndSize() {
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, null, null, -1, 20, "START_ASC"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, null, null, 0, 101, "START_ASC"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsMalformedFromAndTo() {
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, "not-a-date", null, 0, 20, "START_ASC"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, null, "2026-08-01", 0, 20, "START_ASC"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> scheduleQueryService.getSchedules(
                1, null, "2026-08-10T00:00:00Z", "2026-08-01T00:00:00Z", 0, 20, "START_ASC"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void returnsPageInSpecificationShape() {
        OffsetDateTime startAt = OffsetDateTime.parse("2026-08-03T05:30:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-29T01:00:00Z");
        when(scheduleQueryRepository.findSchedules(
                1,
                false,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                "START_ASC",
                0,
                20))
                .thenReturn(new ScheduleListPage(
                        List.of(new ScheduleListRow(
                                301, 1024, "팀 프로젝트 회의",
                                startAt, startAt.plusMinutes(90), false, updatedAt)),
                        1
                ));

        var response = scheduleQueryService.getSchedules(
                1, false, "2026-08-01T00:00:00Z", "2026-09-01T00:00:00Z", 0, 20, " start_asc ");

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.list().getFirst().scheduleId()).isEqualTo(301);
        assertThat(response.list().getFirst().imageId()).isEqualTo(1024);
        assertThat(response.list().getFirst().calendared()).isFalse();
        assertThat(response.list().getFirst().startAt()).isEqualTo(startAt);
    }
}
