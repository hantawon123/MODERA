package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.image.RegisterImagesRequest
import com.ssafy.modera.core.network.model.image.RegisterImagesResponse
import com.ssafy.modera.core.network.model.image.UploadCompleteResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ImageService {

    @POST("api/v1/images/upload")
    suspend fun registerImages(
        @Body request: RegisterImagesRequest,
    ): ApiResponse<BaseResponse<RegisterImagesResponse>>

    @POST("api/v1/images/{image_id}/upload-complete")
    suspend fun notifyUploadComplete(
        @Path("image_id") imageId: Long,
    ): ApiResponse<BaseResponse<UploadCompleteResponse>>
}
