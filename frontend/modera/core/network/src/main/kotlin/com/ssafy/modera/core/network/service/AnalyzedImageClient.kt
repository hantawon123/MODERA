package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.network.mock.AnalyzedImageMockDataSource
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import com.ssafy.modera.core.network.model.analyzedimage.ImageIdsRequest
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
                statuses = query.statuses
                    .map { it.name }
                    .takeIf { it.isNotEmpty() },
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
    ): AnalyzedImageDetailResponse {
        val response = analyzedImageService
            .fetchAnalyzedImageDetail(imageId)
            .getOrThrow()

        return response.data.copy(
            updatedAt = response.timestamp,
        )
    }

    // Todo: api 완성 되면 mock 삭제
    suspend fun fetchRelatedImages(
        imageId: Long,
        limit: Int = 10,
    ): List<AnalyzedImageResponse> =
        analyzedImageService.fetchRelatedImages(
            imageId = imageId,
            limit = limit
        ).getOrThrow().data.list

    suspend fun reanalyzeAnalyzedImage(
        imageId: Long,
    ) = AnalyzedImageMockDataSource.reanalyzeImage(imageId)

    suspend fun deleteAnalyzedImage(
        imageId: List<Long>,
    ) {
        analyzedImageService.deleteAnalyzedImages(
            ImageIdsRequest(
                imageId
            )
        )
    }
}