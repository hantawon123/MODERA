package com.ssafy.modera.api.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record ScheduleCalendarResponse(
        @Schema(description = "일정 ID", example = "301")
        Integer scheduleId,
        @Schema(description = "변경된 캘린더 등록 여부", example = "true")
        boolean calendared,
        @Schema(description = "변경 시각", example = "2026-07-29T06:00:00Z")
        OffsetDateTime updatedAt
) {
}
