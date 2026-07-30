package com.ssafy.modera.worker.domain.category.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "analysis", name = "client", havingValue = "fastapi")
public class FastApiCategoryReanalysisClient implements CategoryReanalysisClient {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private final RestClient restClient;
    private final String internalToken;

    public FastApiCategoryReanalysisClient(
            @Value("${analysis-worker.ai-server-url}") String aiServerUrl,
            @Value("${internal.callback.token}") String internalToken
    ) {
        this.restClient = RestClient.builder().baseUrl(aiServerUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public CategoryResult reanalyze(
            Integer imageId, List<Integer> excludedCategoryIds) {
        CategoryResponse response = restClient.post()
                .uri("/internal/v1/categories/reanalyze")
                .header(HEADER_INTERNAL_TOKEN, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategoryRequest(imageId, excludedCategoryIds))
                .retrieve()
                .body(CategoryResponse.class);
        if (response == null || response.categoryId() == null
                || response.categoryName() == null || response.categoryName().isBlank()) {
            throw new IllegalStateException("AI category response is incomplete");
        }
        if (!imageId.equals(response.imageId())) {
            throw new IllegalStateException("AI category response imageId does not match request");
        }
        return new CategoryResult(response.categoryId(), response.categoryName().trim());
    }

    private record CategoryRequest(
            Integer imageId,
            List<Integer> excludedCategoryIds
    ) {
    }

    private record CategoryResponse(
            Integer imageId,
            Integer categoryId,
            String categoryName
    ) {
    }
}
