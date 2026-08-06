package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.data.mapper.asEntity
import com.ssafy.modera.core.database.dao.DocumentDao
import com.ssafy.modera.core.database.model.asDocument
import com.ssafy.modera.core.database.model.asExternalModel
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import com.ssafy.modera.core.network.model.analyzedimage.asExternalModel
import com.ssafy.modera.core.network.model.document.asExternalModel
import com.ssafy.modera.core.network.service.document.DocumentClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultDocumentRepository @Inject constructor(
    private val documentClient: DocumentClient,
    private val documentDao: DocumentDao,
    @param:Dispatcher(ModeraDispatcher.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : DocumentRepository {

    override fun getDocumentDetail(
        documentId: Long,
    ): Flow<DocumentDetail> =
        documentDao
            .getDocumentWithImageIds(
                documentId = documentId,
            )
            .filterNotNull()
            .map { document ->
                document.asExternalModel()
            }

    override fun getDocuments(
        sortType: DocumentSortType,
    ): Flow<List<Document>> =
        documentDao
            .getDocumentEntities()
            .map { entities ->
                val documents = entities.map { entity ->
                    entity.asDocument()
                }

                when (sortType) {
                    DocumentSortType.LATEST ->
                        documents.sortedByDescending(Document::updatedAt)

                    DocumentSortType.OLDEST ->
                        documents.sortedBy(Document::updatedAt)
                }
            }

    override fun getDocumentImages(
        documentId: Long,
    ): Flow<List<AnalyzedImage>> = flow {
        val response = documentClient.fetchDocumentImages(
            documentId = documentId,
        )

        emit(
            response.map { image ->
                image.asExternalModel()
            },
        )
    }.flowOn(ioDispatcher)

    override suspend fun syncWith(
        resourceId: Long,
    ): Boolean = withContext(ioDispatcher) {
        val response = documentClient.fetchDocumentDetail(
            documentId = resourceId,
        )

        documentDao.upsertDocumentWithImageIds(
            entity = response.asEntity(),
            imageIds = response.imageIds,
        )

        true
    }

    override suspend fun refreshDocumentsIfEmpty() {
        withContext(ioDispatcher) {
            if (documentDao.getDocumentCount() > 0) {
                return@withContext
            }

            val responses = documentClient.fetchDocumentDetails()
            if (responses.isEmpty()) {
                return@withContext
            }

            documentDao.upsertDocumentsWithImageIds(
                documents = responses.map { response ->
                    response.asEntity() to response.imageIds
                },
            )
        }
    }

    override fun regenerateDocument(
        documentId: Long,
        clientRequestId: String,
    ): Flow<DocumentDetail> = flow {
        val response = documentClient.regenerateDocument(
            documentId = documentId,
            clientRequestId = clientRequestId,
            imageIds = null,
        )

        documentDao.upsertDocumentWithImageIds(
            entity = response.asEntity(),
            imageIds = response.imageIds,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun reconstructDocument(
        documentId: Long,
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail> = flow {
        val response = documentClient.regenerateDocument(
            documentId = documentId,
            clientRequestId = clientRequestId,
            imageIds = imageIds,
        )

        documentDao.upsertDocumentWithImageIds(
            entity = response.asEntity(),
            imageIds = response.imageIds,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail> = flow {
        val response = documentClient.createDocument(
            clientRequestId = clientRequestId,
            imageIds = imageIds,
        )

        documentDao.upsertDocumentWithImageIds(
            entity = response.asEntity(),
            imageIds = response.imageIds,
        )

        emit(response.asExternalModel())
    }.flowOn(ioDispatcher)

    override fun deleteDocument(
        documentId: Long,
    ): Flow<Unit> = flow {
        documentClient.deleteDocument(
            documentId = documentId,
        )

        documentDao.deleteDocument(
            documentId = documentId,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)
}