package com.ssafy.modera.core.network.service.document

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.document.CreateDocumentRequest
import com.ssafy.modera.core.network.model.document.DocumentDetailResponse
import javax.inject.Inject

class DocumentClient @Inject constructor(
    private val documentService: DocumentService,
) {

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
}