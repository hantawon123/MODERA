package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable

@Serializable
data class OcrResponse(
    val rawText: String,
    val refinedText: String? = null,
    val lang: String,
    val confidence: Double,
)