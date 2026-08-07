package com.ssafy.modera.api.domain.image.repository;

import java.time.OffsetDateTime;
import java.util.List;

public record ImageListRow(
        Integer imageId,
        String title,
        String summary,
        Boolean favorite,
        String thumbnailKey,
        List<String> tagNames,
        Integer categoryId,
        String categoryName,
        OffsetDateTime uploadedAt,
        Boolean isDocumented,
        Boolean isCalendared
) {
}
