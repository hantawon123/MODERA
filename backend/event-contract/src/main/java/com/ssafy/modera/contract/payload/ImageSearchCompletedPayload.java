package com.ssafy.modera.contract.payload;

import java.util.List;

public record ImageSearchCompletedPayload(
        String correlationId,
        long total,
        int page,
        int size,
        List<Hit> hits
) {
    public record Hit(
            int imageId,
            double score
    ) {
    }
}
