package com.ssafy.modera.core.network.model.analyzedimage

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageOcr
import kotlinx.serialization.Serializable

@Serializable
data class OcrResponse(
    val rawText: String,
    val refinedText: String? = null,
    val lang: String,
    val confidence: Double,
)

fun OcrResponse.asExternalModel(): AnalyzedImageOcr =
    AnalyzedImageOcr(
        rawText = rawText,
        refinedText = refinedText,
        language = lang,
        confidence = confidence,
    )