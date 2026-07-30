package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageResponse(
    val imageId: Long,
    val fileName: String,
    val title: String,
    val summary: String,
    val status: String,
    val favorite: Boolean,
    val thumbnailUrl: String?,
    val tags: List<AnalyzedImageTagResponse>,
    val categories: List<AnalyzedImageCategoryResponse>,
    val createdAt: String,
)

fun AnalyzedImageResponse.asExternalModel(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = "https://i15d207.p.ssafy.io${thumbnailUrl.orEmpty()}",
        hashtags = tags.map { it.name },
        status = when (status) {
            "QUEUED" -> ImageAnalysisStatus.QUEUED
            "PROCESSING" -> ImageAnalysisStatus.PROCESSING
            "COMPLETED" -> ImageAnalysisStatus.COMPLETED
            "FAILED" -> ImageAnalysisStatus.FAILED
            else -> ImageAnalysisStatus.QUEUED
        },
        favorite = favorite,
    )