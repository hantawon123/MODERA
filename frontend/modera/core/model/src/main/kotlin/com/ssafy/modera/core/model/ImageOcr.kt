package com.ssafy.modera.core.model

data class ImageOcr(
    val rawText: String,
    val lang: String,
    val confidence: Double,
)
