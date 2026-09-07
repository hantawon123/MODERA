package com.ssafy.modera.core.network.model.search

import com.ssafy.modera.core.network.BuildConfig

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.serialization.Serializable

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
        thumbnailUrl = thumbnailUrl.toAbsoluteUrl().orEmpty(),
        hashtags = tags,
        favorite = favorite,
    )

private fun String?.toAbsoluteUrl(): String? {
    if (this.isNullOrBlank()) return null
    return if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        "${BuildConfig.API_BASE_URL}$this"
    }
}
