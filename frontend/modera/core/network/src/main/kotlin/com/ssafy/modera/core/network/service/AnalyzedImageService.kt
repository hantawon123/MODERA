package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageFavoriteRequest
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import com.ssafy.modera.core.network.model.analyzedimage.ImageIdsRequest
import com.ssafy.modera.core.network.model.analyzedimage.RelatedImagesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyzedImageService {

    @GET("api/v1/images")
    suspend fun fetchAnalyzedImages(
        @Query("favorite") favorite: Boolean?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
        @Query("keyword") keyword: String?,
        @Query("categoryId") categoryId: Long?,
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

    @POST("api/v1/images/documentize")
    suspend fun fetchDocumentRelatedImages(
        @Body request: ImageIdsRequest,
    ): ApiResponse<BaseResponse<AnalyzedImagesResponse>>

    @PUT("api/v1/images/{imageId}/favorite")
    suspend fun updateAnalyzedImageFavorite(
        @Path("imageId") imageId: Long,
        @Body request: AnalyzedImageFavoriteRequest,
    ): ApiResponse<Unit>

    @POST("api/v1/images/{imageId}/category/reanalysis")
    suspend fun requestImageReanalysis(
        @Path("imageId") imageId: Long,
    ): ApiResponse<Unit>
    @HTTP(
        method = "DELETE",
        path = "api/v1/images",
        hasBody = true,
    )
    suspend fun deleteAnalyzedImages(
        @Body request: ImageIdsRequest,
    ): ApiResponse<Unit>
}