package com.ssafy.modera.core.network.model.search

import kotlinx.serialization.Serializable

@Serializable
data class SemanticSearchRequest(
    val query: String,
    val page: Int = 0,
    val size: Int = 20,
)
