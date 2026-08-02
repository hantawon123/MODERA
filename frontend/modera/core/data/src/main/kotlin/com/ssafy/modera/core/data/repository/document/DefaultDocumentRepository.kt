package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import com.ssafy.modera.core.network.model.document.DocumentSortOption
import com.ssafy.modera.core.network.model.document.asExternalModel
import com.ssafy.modera.core.network.service.document.DocumentClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultDocumentRepository @Inject constructor(
    private val documentClient: DocumentClient,
    @param:Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DocumentRepository {

    override fun getDocumentDetail(
        documentId: Long,
    ): Flow<DocumentDetail> = flow {
        val response = documentClient.fetchDocumentDetail(
            documentId = documentId,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun getDocuments(
        page: Int,
        sortType: DocumentSortType,
        onLastPageReached: () -> Unit,
    ): Flow<List<Document>> = flow {
        val sortOption = when (sortType) {
            DocumentSortType.LATEST -> DocumentSortOption.UPDATED_DESC
            DocumentSortType.OLDEST -> DocumentSortOption.UPDATED_ASC
        }

        val response = documentClient.fetchDocuments(
            page = page,
            sort = sortOption,
        )

        if (!response.hasNext) {
            onLastPageReached()
        }

        emit(
            response.list.map { it.asExternalModel() },
        )
    }.flowOn(ioDispatcher)

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