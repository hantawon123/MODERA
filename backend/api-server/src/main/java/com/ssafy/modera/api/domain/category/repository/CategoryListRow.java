package com.ssafy.modera.api.domain.category.repository;

import java.time.OffsetDateTime;

public record CategoryListRow(
        Integer categoryId,
        String name,
        String imageS3Key,
        Integer imageCount,
        OffsetDateTime latestUploadedAt
) {
}
