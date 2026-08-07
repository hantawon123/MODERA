package com.ssafy.modera.api.domain.image.dto.response;

import java.util.List;

public record ImageListResponse(
        List<ImageSummaryResponse> list,
        int page,
        int size,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {
}
