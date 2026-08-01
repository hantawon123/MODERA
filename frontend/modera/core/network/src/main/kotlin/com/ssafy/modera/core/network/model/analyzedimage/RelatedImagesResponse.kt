package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable

@Serializable
data class RelatedImagesResponse(
    val baseImageId: Long,
    val baseTitle: String,
    val count: Int,
    val list: List<AnalyzedImageResponse>,
)