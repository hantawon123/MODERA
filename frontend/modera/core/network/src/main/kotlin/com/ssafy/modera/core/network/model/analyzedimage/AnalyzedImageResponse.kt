package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageResponse(
    val imageId: Long,
    val fileName: String,
    val title: String,
    val summary: String,
    val status: String,
    val favorite: Boolean,
    val thumbnailUrl: String?,
    val tags: List<AnalyzedImageTagResponse>,
    val categories: List<AnalyzedImageCategoryResponse>,
    val createdAt: String,
)

fun AnalyzedImageResponse.asExternalModel(): AnalyzedImageSummary =
    AnalyzedImageSummary(
        id = imageId,
        title = title,
        imageUrl = thumbnailUrl.orEmpty(),
        hashtags = tags.map(AnalyzedImageTagResponse::name),
    )