package com.ssafy.modera.api.domain.schedule.repository;

import java.util.List;

public record ScheduleListPage(
        List<ScheduleListRow> content,
        long totalElements
) {
}
