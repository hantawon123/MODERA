package com.ssafy.modera.api.domain.image.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ImageSummaryResponse(
        Integer imageId,
        String title,
        String summary,
        Boolean favorite,
        String thumbnailUrl,
        List<String> tags,
        String category,
        OffsetDateTime uploadedAt,
        Boolean isDocumented,
        Boolean isCalendared
) {
}
