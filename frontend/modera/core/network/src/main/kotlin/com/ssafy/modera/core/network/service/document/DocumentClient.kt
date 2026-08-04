package com.ssafy.modera.core.network.service.document

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
import com.ssafy.modera.core.network.model.document.CreateDocumentRequest
import com.ssafy.modera.core.network.model.document.DocumentDetailResponse
import com.ssafy.modera.core.network.model.document.DocumentSortOption
import com.ssafy.modera.core.network.model.document.DocumentsResponse
import com.ssafy.modera.core.network.model.document.RegenerateDocumentRequest
import javax.inject.Inject

class DocumentClient @Inject constructor(
    private val documentService: DocumentService,
) {

    suspend fun fetchDocumentDetail(
        documentId: Long,
    ): DocumentDetailResponse =
        documentService
            .fetchDocumentDetail(documentId)
            .getOrThrow()
            .data

    suspend fun fetchDocuments(
        page: Int,
        sort: DocumentSortOption = DocumentSortOption.UPDATED_DESC,
    ): DocumentsResponse =
        documentService
            .fetchDocuments(
                page = page,
                size = PAGE_SIZE,
                sort = sort.value,
            )
            .getOrThrow()
            .data

    suspend fun fetchDocumentImages(
        documentId: Long,
    ): List<AnalyzedImageResponse> =
        documentService
            .fetchDocumentImages(documentId = documentId)
            .getOrThrow()
            .data

    suspend fun regenerateDocument(
        documentId: Long,
        clientRequestId: String,
        imageIds: List<Long>? = null,
    ): DocumentDetailResponse =
        documentService
            .regenerateDocument(
                documentId = documentId,
                request = RegenerateDocumentRequest(
                    clientRequestId = clientRequestId,
                    imageIds = imageIds,
                ),
            )
            .getOrThrow()
            .data

    suspend fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): DocumentDetailResponse =
        documentService
            .createDocument(
                request = CreateDocumentRequest(
                    clientRequestId = clientRequestId,
                    imageIds = imageIds,
                ),
            )
            .getOrThrow()
            .data

    suspend fun deleteDocument(
        documentId: Long,
    ) {
        documentService
            .deleteDocument(
                documentId = documentId,
            )
            .getOrThrow()
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}