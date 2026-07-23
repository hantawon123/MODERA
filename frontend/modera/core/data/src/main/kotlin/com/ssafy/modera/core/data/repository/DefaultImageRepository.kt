package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.image.RegisterImage
import com.ssafy.modera.core.model.image.RegisterImagesResult
import com.ssafy.modera.core.model.image.UploadCompleteResult
import com.ssafy.modera.core.network.model.image.RegisterImagesRequest
import com.ssafy.modera.core.network.model.image.asExternalModel
import com.ssafy.modera.core.network.model.image.asNetworkModel
import com.ssafy.modera.core.network.service.ImageClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultImageRepository @Inject constructor(
    private val imageClient: ImageClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ImageRepository {

    override fun registerImages(
        images: List<RegisterImage>,
    ): Flow<RegisterImagesResult> = flow {
        val response = imageClient.registerImages(
            RegisterImagesRequest(
                images = images.map { it.asNetworkModel() },
            ),
        )
        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun notifyUploadComplete(
        imageId: Long,
    ): Flow<UploadCompleteResult> = flow {
        emit(imageClient.notifyUploadComplete(imageId).asExternalModel())
    }.flowOn(ioDispatcher)
}
