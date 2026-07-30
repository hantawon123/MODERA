package com.ssafy.modera.worker.domain.category.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "analysis", name = "client",
        havingValue = "mock", matchIfMissing = true)
public class MockCategoryReanalysisClient implements CategoryReanalysisClient {

    @Override
    public CategoryResult reanalyze(
            Integer imageId, List<Integer> excludedCategoryIds) {
        int categoryId = java.util.stream.IntStream.rangeClosed(1, 100)
                .filter(id -> !excludedCategoryIds.contains(id))
                .findFirst()
                .orElseThrow();
        return new CategoryResult(categoryId, "MOCK_CATEGORY_" + categoryId);
    }
}
