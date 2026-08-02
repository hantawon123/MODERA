package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun getDocumentDetail(
        documentId: Long,
    ): Flow<DocumentDetail>

    fun getDocuments(
        page: Int = 0,
        sortType: DocumentSortType = DocumentSortType.LATEST,
        onLastPageReached: () -> Unit,
    ): Flow<List<Document>>

    fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail>

    fun deleteDocument(
        documentId: Long,
    ): Flow<Unit>
}