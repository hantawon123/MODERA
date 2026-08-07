package com.ssafy.modera.contract.payload;

public record ImageSemanticSearchRequestedPayload(
        String correlationId,
        int userId,
        String query,
        int page,
        int size
) {
}
