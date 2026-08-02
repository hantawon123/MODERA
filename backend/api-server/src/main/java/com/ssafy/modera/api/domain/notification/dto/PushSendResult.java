package com.ssafy.modera.api.domain.notification.dto;

public record PushSendResult(
        boolean enabled,
        int targetCount,
        int successCount,
        int failureCount
) {
    public static PushSendResult disabled(int targetCount) {
        return new PushSendResult(false, targetCount, 0, 0);
    }
}
