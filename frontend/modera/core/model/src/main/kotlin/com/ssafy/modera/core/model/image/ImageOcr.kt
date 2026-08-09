package com.ssafy.modera.core.model.image

data class ImageOcr(
    val rawText: String,
    val lang: String,
    val confidence: Double,
)
