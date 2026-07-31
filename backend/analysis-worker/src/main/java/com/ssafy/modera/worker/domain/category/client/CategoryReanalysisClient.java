package com.ssafy.modera.worker.domain.category.client;

import java.util.List;

public interface CategoryReanalysisClient {
    CategoryResult reanalyze(
            Integer userId,
            Integer imageId,
            List<Integer> excludedCategoryIds);

    record CategoryResult(Integer categoryId, String categoryName) {
    }
}
