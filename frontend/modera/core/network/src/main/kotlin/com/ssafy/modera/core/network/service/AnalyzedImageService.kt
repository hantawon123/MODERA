package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import com.ssafy.modera.core.network.model.analyzedimage.DeleteAnalyzedImagesRequest
import com.ssafy.modera.core.network.model.analyzedimage.RelatedImagesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyzedImageService {

    @GET("api/v1/images")
    suspend fun fetchAnalyzedImages(
        @Query("status") statuses: List<String>?,
        @Query("categoryId") categoryId: Long?,
        @Query("tagId") tagId: Long?,
        @Query("favorite") favorite: Boolean?,
        @Query("dateFrom") dateFrom: String?,
        @Query("dateTo") dateTo: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): ApiResponse<BaseResponse<AnalyzedImagesResponse>>

    @GET("api/v1/images/{imageId}")
    suspend fun fetchAnalyzedImageDetail(
        @Path("imageId") imageId: Long,
    ): ApiResponse<BaseResponse<AnalyzedImageDetailResponse>>

    @GET("api/v1/images/{imageId}/similar")
    suspend fun fetchRelatedImages(
        @Path("imageId") imageId: Long,
        @Query("limit") limit: Int,
    ): ApiResponse<BaseResponse<RelatedImagesResponse>>

    @HTTP(
        method = "DELETE",
        path = "api/v1/images",
        hasBody = true,
    )
    suspend fun deleteAnalyzedImages(
        @Body request: DeleteAnalyzedImagesRequest,
    ): ApiResponse<Unit>
}