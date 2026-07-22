package com.ssafy.modera.contract.payload;

/**
 * {@link com.ssafy.modera.contract.EventTypes#ANALYSIS_FAILED} 이벤트 payload.
 */
public record AnalysisFailedPayload(
        String imageId,
        long userId,
        String errorCode,
        String errorMessage,
        boolean retryable
) {
}
