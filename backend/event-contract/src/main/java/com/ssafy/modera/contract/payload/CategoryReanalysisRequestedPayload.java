package com.ssafy.modera.contract.payload;

import java.util.List;
import java.util.UUID;

public record CategoryReanalysisRequestedPayload(
        UUID categoryRequestId,
        int userId,
        int imageId,
        List<Integer> excludedCategoryIds
) {
}
