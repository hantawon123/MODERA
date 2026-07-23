package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnalyzedImageDetailResponse(
    val imageId: Long,
    val fileName: String,
    val contentHash: String,
    val status: String,
    val favorite: Boolean,
    val title: String,
    val summary: String,
    val ocr: OcrResponse?,
    val tags: List<AnalyzedImageTagResponse>,
    val categories: List<AnalyzedImageCategoryResponse>,
    val analysisConfidence: Double?,
    val imageUrl: String,
    val createdAt: String,
    val uploadedAt: String?,
    val updatedAt: String,
    val lastViewedAt: String?,
)