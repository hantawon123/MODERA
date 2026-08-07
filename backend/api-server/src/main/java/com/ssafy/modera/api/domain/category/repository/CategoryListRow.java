package com.ssafy.modera.api.domain.category.repository;

import java.time.OffsetDateTime;

public record CategoryListRow(
        Integer categoryId,
        String name,
        Integer imageCount,
        OffsetDateTime latestUploadedAt
) {
}
