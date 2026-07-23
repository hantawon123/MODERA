package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class OcrRequest(
    val rawText: String,
    val lang: String,
    val confidence: Double,
)
