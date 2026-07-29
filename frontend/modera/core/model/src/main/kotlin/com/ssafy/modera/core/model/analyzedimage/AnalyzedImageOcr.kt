package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageOcr(
    val rawText: String,
    val refinedText: String?,
    val confidence: Double,
)