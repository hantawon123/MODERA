package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 5-10 전체 동기화 응답. 페이지 없이 전 항목을 한 번에 준다. */
public record ImageSyncResponse(
        List<ImageSyncItemResponse> list,
        @Schema(description = "list의 길이") long totalElements
) {
}
