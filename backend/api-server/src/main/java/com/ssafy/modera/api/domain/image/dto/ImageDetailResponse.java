package com.ssafy.modera.api.domain.image.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageDetailResponse(
        UUID imageId,
        String s3Key,
        String uploadStatus,
        String analysisStatus,
        String title,
        Boolean favorite,
        OffsetDateTime createdAt
) {
}
