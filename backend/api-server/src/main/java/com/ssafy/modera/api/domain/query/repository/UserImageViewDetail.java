package com.ssafy.modera.api.domain.query.repository;

import java.time.OffsetDateTime;

public record UserImageViewDetail(
        String analysisStatus,
        String title,
        Boolean favorite,
        OffsetDateTime createdAt
) {
}
