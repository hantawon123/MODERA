package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageCategoryResponse(
    val categoryId: Long,
    val name: String,
)