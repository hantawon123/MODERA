package com.ssafy.modera.core.network.model.document

import kotlinx.serialization.Serializable

@Serializable
data class CreateDocumentRequest(
    val clientRequestId: String,
    val imageIds: List<Long>,
)