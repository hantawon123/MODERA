package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 5-10 전체 동기화 페이지. 목록(ImageListResponse)과 같은 페이지 메타 구성이다. */
public record ImageSyncResponse(
        List<ImageSyncItemResponse> list,
        @Schema(description = "페이지 번호(0부터)") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "동기화 대상 전체 건수") long totalElements,
        @Schema(description = "다음 페이지 존재 여부 — false가 나올 때까지 page를 올려 호출한다") boolean hasNext,
        boolean hasPrevious
) {
}
