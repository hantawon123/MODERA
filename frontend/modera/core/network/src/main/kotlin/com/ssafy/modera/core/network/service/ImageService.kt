package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.RegisterImagesRequest
import com.ssafy.modera.core.network.model.RegisterImagesResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ImageService {

    @POST("api/v1/images/upload")
    suspend fun registerImages(
        @Body request: RegisterImagesRequest,
    ): ApiResponse<BaseResponse<RegisterImagesResponse>>
}
