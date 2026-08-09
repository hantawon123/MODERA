package com.ssafy.modera.core.network.model.category

import kotlinx.serialization.Serializable

@Serializable
data class CategoryTagResponse(
    val tagId: Long,
    val name: String,
    val imageCount: Int,
)