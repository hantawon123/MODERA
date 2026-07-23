package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyzedImageService {

    @GET("api/v1/images")
    suspend fun fetchAnalyzedImages(
        @Query("status") statuses: Set<ImageAnalysisStatus>?,
        @Query("categoryId") categoryId: Long?,
        @Query("tagId") tagId: Long?,
        @Query("favorite") favorite: Boolean?,
        @Query("dateFrom") dateFrom: String?,
        @Query("dateTo") dateTo: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): ApiResponse<BaseResponse<AnalyzedImagesResponse>>
}