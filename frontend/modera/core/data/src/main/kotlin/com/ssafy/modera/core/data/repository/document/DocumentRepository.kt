package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.data.sync.Syncable
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import kotlinx.coroutines.flow.Flow

interface DocumentRepository : Syncable {

    fun getDocumentDetail(
        documentId: Long,
    ): Flow<DocumentDetail>

    fun getAnalyzedImagesByDocumentId(
        documentId: Long,
    ): Flow<List<AnalyzedImage>>

    fun getDocuments(
        sortType: DocumentSortType = DocumentSortType.LATEST,
    ): Flow<List<Document>>

    fun regenerateDocument(
        documentId: Long,
        clientRequestId: String,
    ): Flow<DocumentDetail>

    fun reconstructDocument(
        documentId: Long,
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail>

    fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail>

    fun deleteDocument(
        documentId: Long,
    ): Flow<Unit>

    suspend fun refreshDocumentsIfEmpty()
}