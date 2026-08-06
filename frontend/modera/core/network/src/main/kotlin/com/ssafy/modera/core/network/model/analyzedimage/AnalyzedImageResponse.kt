package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageResponse(
    val imageId: Long,
    val title: String,
    val summary: String,
    val favorite: Boolean = false,
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val categoryId: Long = 0L,
    val category: String = "",
    val uploadedAt: String? = null,
    val isDocumented: Boolean = false,
    val isCalendared: Boolean = false,
)

fun AnalyzedImageResponse.asExternalModel(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl.orEmpty(),
        hashtags = tags,
        favorite = favorite,
        isDocumented = isDocumented,
        hasSchedule = isCalendared,
    )