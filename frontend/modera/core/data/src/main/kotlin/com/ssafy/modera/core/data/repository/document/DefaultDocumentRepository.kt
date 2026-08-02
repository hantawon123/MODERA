package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.network.model.document.asExternalModel
import com.ssafy.modera.core.network.service.document.DocumentClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultDocumentRepository @Inject constructor(
    private val documentClient: DocumentClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DocumentRepository {

    override fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail> = flow {
        val response = documentClient.createDocument(
            clientRequestId = clientRequestId,
            imageIds = imageIds,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)
}