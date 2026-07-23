package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import javax.inject.Inject

class AnalyzedImageClient @Inject constructor(
    private val analyzedImageService: AnalyzedImageService,
) {

    suspend fun fetchAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery = AnalyzedImageQuery(),
    ): AnalyzedImagesResponse =
        analyzedImageService
            .fetchAnalyzedImages(
                statuses = query.statuses.takeIf { it.isNotEmpty() },
                categoryId = query.categoryId,
                tagId = query.tagId,
                favorite = query.favorite,
                dateFrom = query.dateFrom,
                dateTo = query.dateTo,
                page = page,
                size = 20,
                sort = "createdAt,desc",
            )
            .getOrThrow()
            .data

    suspend fun fetchAnalyzedImageDetail(
        imageId: Long,
    ): AnalyzedImageDetailResponse =
        analyzedImageService
            .fetchAnalyzedImageDetail(imageId)
            .getOrThrow()
            .data
}