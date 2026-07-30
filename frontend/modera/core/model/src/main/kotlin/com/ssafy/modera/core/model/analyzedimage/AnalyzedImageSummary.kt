package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageSummary(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
    val hashtags: List<String>,
    val status: ImageAnalysisStatus,
    val favorite: Boolean,
)