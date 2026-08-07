package com.ssafy.modera.api.domain.notification.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDataChangeOutboxEvent(
        UUID outboxId,
        Integer userId,
        String resource,
        String resourceId,
        int retryCount,
        OffsetDateTime createdAt
) {
}
