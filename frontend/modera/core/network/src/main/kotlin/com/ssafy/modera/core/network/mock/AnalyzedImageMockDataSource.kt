package com.ssafy.modera.core.network.mock

import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageCategoryResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageTagResponse
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal object AnalyzedImageMockDataSource {

    suspend fun fetchRelatedImages(
        sourceImageId: Long,
    ): List<AnalyzedImageResponse> {
        validateImageId(sourceImageId)
        delay(RelatedImagesDelay)

        return createRelatedImages(
            sourceImageId = sourceImageId,
        )
    }

    suspend fun reanalyzeImage(
        imageId: Long,
    ) {
        validateImageId(imageId)
        delay(ReanalyzeDelay)
    }

    suspend fun deleteImage(
        imageId: Long,
    ) {
        validateImageId(imageId)
        delay(DeleteDelay)
    }

    private fun createRelatedImages(
        sourceImageId: Long,
    ): List<AnalyzedImageResponse> =
        List(RelatedImageCount) { index ->
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

    private fun validateImageId(
        imageId: Long,
    ) {
        require(imageId > 0L) {
            "AnalyzedImage id must be greater than 0."
        }
    }

    private val RelatedImagesDelay = 300L.milliseconds
    private val DeleteDelay = 500L.milliseconds
    private val ReanalyzeDelay = 3000L.milliseconds

    private const val RelatedImageCount = 10
}