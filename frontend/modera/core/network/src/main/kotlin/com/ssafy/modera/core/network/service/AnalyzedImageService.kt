package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.AnalyzedImagesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyzedImageService {

    @GET("api/v1/images")
    suspend fun fetchAnalyzedImages(
        @Query("status") status: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("tagId") tagId: Long? = null,
        @Query("favorite") favorite: Boolean? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "createdAt,desc",
    ): ApiResponse<AnalyzedImagesResponse>
}