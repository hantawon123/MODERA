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
    val categoryId: Long,
    val category: String,
    val tags: List<String>,
    val keyInformation: List<String>,
    val ocrRefinedText: String? = null,
    val isDocumented: Boolean,
    val isCalendared: Boolean,
    val uploadedAt: String? = null,
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
        extractedTexts = ocrRefinedText.orEmpty()
            .split(Regex("\\s+"))
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { text -> text.any(Char::isLetterOrDigit) },
        keyInformation = keyInformation,
        isDocumented = isDocumented,
        isCalendared = isCalendared,
        updatedAt = uploadedAt
            ?.let(Instant::parse)
            ?.toEpochMilli()
            ?: 0L,
    )