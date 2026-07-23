package com.ssafy.modera.media

import android.content.Context
import com.ssafy.modera.core.model.ImageOcr
import com.ssafy.modera.core.model.RegisterImage
import java.util.UUID

private const val DEFAULT_OCR_LANG = "ko"
private const val DEFAULT_OCR_CONFIDENCE = 0.0

fun SelectedImage.toRegisterImage(
    context: Context,
    clientRequestId: String = UUID.randomUUID().toString(),
): RegisterImage =
    RegisterImage(
        clientRequestId = clientRequestId,
        fileName = originalFileName,
        contentHash = uri.sha256(context.contentResolver),
        fileSize = fileSizeBytes.coerceAtLeast(0L),
        ocr = ImageOcr(
            rawText = ocrText,
            lang = DEFAULT_OCR_LANG,
            confidence = DEFAULT_OCR_CONFIDENCE,
        ),
    )
