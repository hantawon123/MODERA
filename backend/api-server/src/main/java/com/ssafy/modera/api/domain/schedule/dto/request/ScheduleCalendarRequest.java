package com.ssafy.modera.api.domain.schedule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ScheduleCalendarRequest(
        @Schema(description = "캘린더 등록 여부", example = "true")
        @NotNull(message = "필수 값입니다.")
        Boolean calendared
) {
}
