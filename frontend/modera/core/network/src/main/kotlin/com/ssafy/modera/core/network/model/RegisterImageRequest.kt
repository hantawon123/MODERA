package com.ssafy.modera.core.network.model

import com.ssafy.modera.core.model.ImageOcr
import com.ssafy.modera.core.model.RegisterImage
import kotlinx.serialization.Serializable

@Serializable
data class RegisterImageRequest(
    val clientRequestId: String,
    val fileName: String,
    val contentHash: String,
    val fileSize: Long,
    val ocr: OcrRequest,
)

fun RegisterImage.asNetworkModel(): RegisterImageRequest =
    RegisterImageRequest(
        clientRequestId = clientRequestId,
        fileName = fileName,
        contentHash = contentHash,
        fileSize = fileSize,
        ocr = ocr.asNetworkModel(),
    )

fun ImageOcr.asNetworkModel(): OcrRequest =
    OcrRequest(
        rawText = rawText,
        lang = lang,
        confidence = confidence,
    )
