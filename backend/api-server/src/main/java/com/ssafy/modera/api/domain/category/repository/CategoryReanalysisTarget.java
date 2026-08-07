package com.ssafy.modera.api.domain.category.repository;

import java.util.List;

public record CategoryReanalysisTarget(
        Integer userImageId,
        List<Integer> excludedCategoryIds
) {
}
