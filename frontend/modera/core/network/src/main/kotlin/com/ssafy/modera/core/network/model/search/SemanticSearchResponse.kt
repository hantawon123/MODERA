package com.ssafy.modera.core.network.model.search

import kotlinx.serialization.Serializable

@Serializable
data class SemanticSearchResponse(
    val list: List<SemanticSearchImageResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)
