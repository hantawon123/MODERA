package com.ssafy.modera.api.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record ScheduleSummaryResponse(
        @Schema(description = "일정 ID", example = "301")
        Integer scheduleId,
        @Schema(description = "출처 이미지 ID(이미지가 삭제된 뒤에도 유지되므로 상세 조회 시 404가 될 수 있다)", example = "1024")
        Integer imageId,
        @Schema(description = "일정 제목", example = "팀 프로젝트 회의")
        String title,
        @Schema(description = "시작 시각(분석에서 추출하지 못하면 null)", example = "2026-08-03T05:30:00Z")
        OffsetDateTime startAt,
        @Schema(description = "종료 시각(분석에서 추출하지 못하면 null)", example = "2026-08-03T07:00:00Z")
        OffsetDateTime endAt,
        @Schema(description = "캘린더 등록 여부", example = "false")
        boolean calendared,
        @Schema(description = "마지막 변경 시각", example = "2026-07-29T01:00:00Z")
        OffsetDateTime updatedAt
) {
}
