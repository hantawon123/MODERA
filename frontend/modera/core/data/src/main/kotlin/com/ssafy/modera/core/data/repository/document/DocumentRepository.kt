package com.ssafy.modera.core.data.repository.document

import com.ssafy.modera.core.model.DocumentDetail
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun createDocument(
        clientRequestId: String,
        imageIds: List<Long>,
    ): Flow<DocumentDetail>
}