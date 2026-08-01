package com.ssafy.modera.core.network.service.search

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.search.SemanticSearchRequest
import com.ssafy.modera.core.network.model.search.SemanticSearchResponse
import javax.inject.Inject

class SearchClient @Inject constructor(
    private val searchService: SearchService,
) {
    suspend fun searchSemanticImages(
        query: String,
        page: Int = 0,
        size: Int = 20,
    ): SemanticSearchResponse =
        searchService
            .searchSemanticImages(
                SemanticSearchRequest(
                    query = query,
                    page = page,
                    size = size,
                ),
            )
            .getOrThrow()
            .data
}
