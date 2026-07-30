package com.ssafy.modera.api.domain.category.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryReanalysisResponse(
        UUID categoryRequestId,
        Integer imageId,
        List<Integer> excludedCategoryIds,
        String status
) {
}
