package com.ssafy.modera.core.model

data class DocumentDetail(
    val id: Long,
    val name: String,
    val summary: String,
    val content: String,
    val imageCount: Int,
    val deletedImageCount: Int,
    val imageIds: List<Long>,
    val regenerating: Boolean,
    val updatedAt: Long,
)