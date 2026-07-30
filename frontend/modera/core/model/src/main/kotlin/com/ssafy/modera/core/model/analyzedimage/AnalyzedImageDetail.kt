package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageDetail(
    val id: Long,
    val fileName: String,
    val status: ImageAnalysisStatus,
    val favorite: Boolean,
    val title: String,
    val summary: String,
    val ocr: AnalyzedImageOcr?,
    val tags: List<String>,
    val categories: AnalyzedImageCategory,
    val imageUrl: String,
    val updatedAt: Long,
)