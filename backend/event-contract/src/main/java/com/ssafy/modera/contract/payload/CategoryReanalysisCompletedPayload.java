package com.ssafy.modera.contract.payload;

import java.util.UUID;

public record CategoryReanalysisCompletedPayload(
        UUID categoryRequestId,
        int userId,
        int imageId,
        int categoryId,
        String categoryName
) {
}
