package com.ssafy.modera.core.network.model.document

import kotlinx.serialization.Serializable

@Serializable
data class DocumentsResponse(
    val list: List<DocumentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)