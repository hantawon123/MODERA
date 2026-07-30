package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageCategoryResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageTagResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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
    ): AnalyzedImageDetailResponse =
        analyzedImageService
            .fetchAnalyzedImageDetail(imageId)
            .getOrThrow()
            .data

    suspend fun fetchRelatedImages(
        imageId: Long,
    ): List<AnalyzedImageResponse> {
        delay(300L.milliseconds)

        return createMockRelatedImages(
            sourceImageId = imageId,
        )
    }
}

private fun createMockRelatedImages(
    sourceImageId: Long,
): List<AnalyzedImageResponse> =
    List(10) { index ->
        val sequence = index + 1

        AnalyzedImageResponse(
            imageId = sourceImageId * 300L + sequence,
            fileName = "related_image_$sequence.png",
            title = "성심당 케이크 리스트",
            summary = "성심당의 케이크 메뉴와 가격, 예약 정보를 정리한 이미지입니다.",
            status = "COMPLETED",
            favorite = false,
            thumbnailUrl =
                "https://picsum.photos/seed/related-$sourceImageId-$sequence/300/300",
            tags = listOf(
                AnalyzedImageTagResponse(
                    tagId = 1L,
                    name = "성심당",
                ),
                AnalyzedImageTagResponse(
                    tagId = 2L,
                    name = "케이크",
                ),
                AnalyzedImageTagResponse(
                    tagId = 3L,
                    name = "예약",
                ),
            ),
            categories = listOf(
                AnalyzedImageCategoryResponse(
                    categoryId = 1L,
                    name = "음식",
                ),
            ),
            createdAt = "2026-07-30T07:00:00.000Z",
        )
    }
