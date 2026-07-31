package com.ssafy.modera.core.network.model.image

import kotlinx.serialization.Serializable

@Serializable
data class ImageListResponse(
    val data: ImageListData,
)

@Serializable
data class ImageListData(
    val list: List<ImageItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

@Serializable
data class ImageItemResponse(
    val imageId: Int,
    val title: String,
    val summary: String,
    val favorite: Boolean,
    val thumbnailUrl: String?,
    val tags: List<String>,
    val category: String,
    val uploadedAt: String,
)