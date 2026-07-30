package com.ssafy.modera.contract.payload;

import java.util.UUID;

public record CategoryReanalysisFailedPayload(
        UUID categoryRequestId,
        int userId,
        int imageId,
        String errorCode,
        String message,
        boolean retryable
) {
}
