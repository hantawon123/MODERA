package com.ssafy.modera.contract.payload;

public record ImageSearchFailedPayload(
        String correlationId,
        String reason
) {
}
