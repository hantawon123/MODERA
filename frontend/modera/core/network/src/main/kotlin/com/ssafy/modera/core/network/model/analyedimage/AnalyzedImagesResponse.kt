package com.ssafy.modera.core.network.model.analyedimage

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImagesResponse(
    val list: List<AnalyzedImageResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)