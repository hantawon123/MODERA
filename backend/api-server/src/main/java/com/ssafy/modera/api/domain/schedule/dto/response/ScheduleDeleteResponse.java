package com.ssafy.modera.api.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScheduleDeleteResponse(
        @Schema(description = "삭제한 일정 ID", example = "301")
        Integer scheduleId,
        @Schema(description = "삭제 성공 여부", example = "true")
        boolean deleted
) {
}
