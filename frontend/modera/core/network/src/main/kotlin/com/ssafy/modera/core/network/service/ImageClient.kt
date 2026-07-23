package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.RegisterImagesRequest
import com.ssafy.modera.core.network.model.RegisterImagesResponse
import javax.inject.Inject

class ImageClient @Inject constructor(
    private val imageService: ImageService,
) {
    suspend fun registerImages(
        request: RegisterImagesRequest,
    ): RegisterImagesResponse {
        val response = imageService.registerImages(request).getOrThrow()
        return response.data
    }
}
