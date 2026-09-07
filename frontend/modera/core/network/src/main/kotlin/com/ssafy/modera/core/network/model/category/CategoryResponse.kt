package com.ssafy.modera.core.network.model.category

import com.ssafy.modera.core.network.BuildConfig

import com.ssafy.modera.core.model.category.Category
import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val categoryId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val imageCount: Int,
    val tags: List<CategoryTagResponse>,
    val updatedAt: String,
)

fun CategoryResponse.asExternalModel(): Category =
    Category(
        id = categoryId,
        title = name,
        thumbnailUrl = "${BuildConfig.MEDIA_BASE_URL.trimEnd('/')}$thumbnailUrl",
        itemCount = imageCount,
        tags = tags.map { tag ->
            tag.name
        },
    )