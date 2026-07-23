package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.AnalyzedImagesRequest
import com.ssafy.modera.core.network.model.AnalyzedImagesResponse
import javax.inject.Inject

class AnalyzedImageClient @Inject constructor(
    private val analyzedImageService: AnalyzedImageService,
) {

    suspend fun fetchAnalyzedImages(
        request: AnalyzedImagesRequest = AnalyzedImagesRequest(),
    ): AnalyzedImagesResponse =
        analyzedImageService
            .fetchAnalyzedImages(
                status = request.statusQuery,
                categoryId = request.categoryId,
                tagId = request.tagId,
                favorite = request.favorite,
                dateFrom = request.dateFrom,
                dateTo = request.dateTo,
                page = request.page,
                size = request.size,
            )
            .getOrThrow()
}