package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.network.model.analyzedimage.asExternalModel
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class DefaultAnalyzedImageRepository @Inject constructor(
    private val analyzedImageClient: AnalyzedImageClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AnalyzedImageRepository {

    override fun getAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery,
    ): Flow<List<AnalyzedImage>> = flow {
        val response = analyzedImageClient.fetchAnalyzedImages(
            page = page,
            query = query,
        )

        emit(response.list.map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getAnalyzedImageDetail(
        imageId: Long,
    ): Flow<AnalyzedImageDetail> = flow {
        val response = analyzedImageClient.fetchAnalyzedImageDetail(
            imageId = imageId,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun getRelatedImages(
        imageId: Long,
    ): Flow<List<AnalyzedImage>> = flow {
        val relatedImages = analyzedImageClient
            .fetchRelatedImages(imageId)
            .map { response ->
                response.asExternalModel()
            }

        emit(relatedImages)
    }.flowOn(ioDispatcher)

    override fun getDocumentRecommendedImages(
        selectedImageIds: List<Long>,
    ): Flow<List<AnalyzedImage>> = flow {
        delay(500L.milliseconds)

        val selectedImageIdSet = selectedImageIds.toSet()

        val recommendedImages = mockDocumentRecommendedImages
            .filterNot { image ->
                image.id in selectedImageIdSet
            }
            .take(10)

        emit(recommendedImages)
    }.flowOn(ioDispatcher)

    override fun reanalyzeAnalyzedImage(
        imageId: Long,
    ): Flow<Unit> = flow {
        analyzedImageClient.reanalyzeAnalyzedImage(
            imageId = imageId,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)

    override fun deleteAnalyzedImage(
        imageId: Long,
    ): Flow<Unit> = flow {
        analyzedImageClient.deleteAnalyzedImage(
            imageId = imageId,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)
}

// Todo: network 연결 후 삭제
private val mockDocumentRecommendedImages = List(20) { index ->
    AnalyzedImage(
        id = 10_000L + index,
        title = "성심당 케이크 리스트 ${index + 1}",
        summary = "성심당 케이크 메뉴와 가격, 예약 정보를 정리한 이미지입니다.",
        thumbnailUrl =
            "https://picsum.photos/seed/" +
                    "document-recommendation-$index/300/300",
        hashtags = listOf(
            "성심당",
            "케이크",
            "예약",
        ),
        status = ImageAnalysisStatus.COMPLETED,
        favorite = false,
    )
}