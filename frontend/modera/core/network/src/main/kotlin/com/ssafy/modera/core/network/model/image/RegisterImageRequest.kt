package com.ssafy.modera.core.network.model.image

import com.ssafy.modera.core.model.image.ImageOcr
import com.ssafy.modera.core.model.image.RegisterImage
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
