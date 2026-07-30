package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary
import com.ssafy.modera.core.network.model.analyzedimage.asExternalModel
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultAnalyzedImageRepository @Inject constructor(
    private val analyzedImageClient: AnalyzedImageClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AnalyzedImageRepository {

    override fun getAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery,
    ): Flow<List<AnalyzedImageSummary>> = flow {
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
    ): Flow<List<AnalyzedImageSummary>> = flow {
        val relatedImages = analyzedImageClient
            .fetchRelatedImages(imageId)
            .map { response ->
                response.asExternalModel()
            }

        emit(relatedImages)
    }.flowOn(ioDispatcher)
}