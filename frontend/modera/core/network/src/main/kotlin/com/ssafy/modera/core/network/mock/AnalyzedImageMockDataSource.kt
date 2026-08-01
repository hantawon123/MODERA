package com.ssafy.modera.core.network.mock

import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
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
                title = "성심당 케이크 리스트",
                summary = "성심당의 케이크 메뉴와 가격, 예약 정보를 정리한 이미지입니다.",
                favorite = false,
                thumbnailUrl =
                    "https://picsum.photos/seed/related-$sourceImageId-$sequence/300/300",
                tags = listOf("성심당", "예약", "케이크"),
                category = "음식",
                uploadedAt = "2026-07-30T07:00:00.000Z",
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