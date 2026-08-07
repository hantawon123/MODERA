package com.ssafy.modera.api.domain.schedule.repository;

import java.time.OffsetDateTime;

public record ScheduleListRow(
        Integer scheduleId,
        Integer imageId,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean calendared,
        OffsetDateTime updatedAt
) {
}
