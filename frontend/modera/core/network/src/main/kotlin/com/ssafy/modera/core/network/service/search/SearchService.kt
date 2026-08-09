package com.ssafy.modera.core.network.service.search

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.search.SemanticSearchRequest
import com.ssafy.modera.core.network.model.search.SemanticSearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SearchService {

    @POST("api/v1/images/search/semantic")
    suspend fun searchSemanticImages(
        @Body request: SemanticSearchRequest,
    ): ApiResponse<BaseResponse<SemanticSearchResponse>>
}
