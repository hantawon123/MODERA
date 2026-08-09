package com.ssafy.modera.core.network.model.search

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SemanticSearchImageResponse(
    val imageId: Long,
    val title: String,
    val summary: String,
    val favorite: Boolean,
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val uploadedAt: String,
)

fun SemanticSearchImageResponse.asExternalModel(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = "https://i15d207.p.ssafy.io:8443/api/v1/images/${imageId}/thumbnail/raw",
        hashtags = tags,
        favorite = favorite,
        updatedAt = runCatching { Instant.parse(uploadedAt).toEpochMilli() }
            .getOrDefault(0L),
    )