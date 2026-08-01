package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageResponse(
    val imageId: Long,
    val title: String,
    val summary: String,
    val favorite: Boolean,
    val thumbnailUrl: String,
    val tags: List<String>,
    val category: String,
    val createdAt: String? = null,
)

fun AnalyzedImageResponse.asExternalModel(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl,
        hashtags = tags,
        favorite = favorite,
    )