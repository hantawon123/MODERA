package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AnalyzedImageDetailResponse(
    val imageId: Long,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val title: String,
    val favorite: Boolean,
    val summary: String,
    val category: String,
    val tags: List<String>,
    val keyInformation: List<String>,
    val isDocumented: Boolean,
    val isCalendared: Boolean,
    val updatedAt: String? = null,
)

fun AnalyzedImageDetailResponse.asExternalModel(): AnalyzedImageDetail =
    AnalyzedImageDetail(
        id = imageId,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl.orEmpty(),
        title = title,
        favorite = favorite,
        summary = summary,
        category = category,
        tags = tags,
        extractedTexts = emptyList(),
        keyInformation = keyInformation,
        isDocumented = isDocumented,
        isCalendared = isCalendared,
        updatedAt = updatedAt
            ?.let(Instant::parse)
            ?.toEpochMilli()
            ?: 0L,
    )