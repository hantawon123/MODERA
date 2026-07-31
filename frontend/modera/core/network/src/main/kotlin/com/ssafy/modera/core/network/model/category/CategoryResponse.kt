package com.ssafy.modera.core.network.model.category

import com.ssafy.modera.core.model.category.Category
import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val categoryId: Long,
    val name: String,
    val categoryImageUrl: String? = null,
    val imageCount: Int,
    val latestUpdatedAt: String? = null,
)

fun CategoryResponse.asExternalModel(): Category =
    Category(
        id = categoryId,
        title = name,
        thumbnailUrl = categoryImageUrl.toAbsoluteUrl(),
        itemCount = imageCount,
        tags = emptyList(),
    )

private fun String?.toAbsoluteUrl(): String? {
    if (this.isNullOrBlank()) return null
    return if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        "https://i15d207.p.ssafy.io:8443/$this"
    }
}
