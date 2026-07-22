package com.ssafy.modera.api.domain.image.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageRegisterResponse(
        UUID imageId,
        String presignedPutUrl,
        String s3Key,
        OffsetDateTime expiresAt
) {
}
