package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImagesResponse(
    val list: List<AnalyzedImageResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int = 0,
    val hasNext: Boolean,
    val hasPrevious: Boolean = false,
)