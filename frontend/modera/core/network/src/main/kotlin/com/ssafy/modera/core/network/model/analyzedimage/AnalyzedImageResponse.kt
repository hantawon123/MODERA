package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.serialization.Serializable
import java.time.Instant

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
        thumbnailUrl = "https://i15d207.p.ssafy.io:8443/api/v1/images/${imageId}/thumbnail/raw",
        hashtags = tags,
        favorite = favorite,
        isDocumented = isDocumented,
        hasSchedule = isCalendared,
        updatedAt = uploadedAt
            ?.let { value ->
                runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            }
            ?: 0L,
    )
