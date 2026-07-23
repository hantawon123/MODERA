package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageCategory
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import kotlinx.serialization.Serializable

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

// Todo: categories -> 단일 객체로 서버에 수정 요청
fun AnalyzedImageDetailResponse.asExternalModel(): AnalyzedImageDetail =
    AnalyzedImageDetail(
        id = imageId,
        fileName = fileName,
        status = when (status) {
            "QUEUED" -> ImageAnalysisStatus.QUEUED
            "PROCESSING" -> ImageAnalysisStatus.PROCESSING
            "COMPLETED" -> ImageAnalysisStatus.COMPLETED
            "FAILED" -> ImageAnalysisStatus.FAILED
            else -> ImageAnalysisStatus.QUEUED
        },
        favorite = favorite,
        title = title,
        summary = summary,
        ocr = ocr?.asExternalModel(),
        tags = tags.map { it.name },
        categories = AnalyzedImageCategory(
            categories.first().categoryId,
            categories.first().name
        ),
        imageUrl = "https://i15d207.p.ssafy.io$imageUrl",
        createdAt = createdAt,
        updatedAt = updatedAt,
    )