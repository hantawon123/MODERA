package com.ssafy.modera.core.model.image

data class RegisterImage(
    val clientRequestId: String,
    val fileName: String,
    val contentHash: String,
    val fileSize: Long,
    val ocr: ImageOcr,
)
