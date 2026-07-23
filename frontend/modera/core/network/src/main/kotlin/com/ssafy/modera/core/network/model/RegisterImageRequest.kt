package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterImageRequest(
    val clientRequestId: String,
    val fileName: String,
    val contentHash: String,
    val fileSize: Long,
    val ocr: OcrRequest,
)
