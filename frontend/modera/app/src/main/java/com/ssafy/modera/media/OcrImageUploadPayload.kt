package com.ssafy.modera.media

import kotlinx.serialization.Serializable

/**
 * 서버로 보낼 OCR 이미지 등록 페이로드.
 * 실제 전송 로직은 별도 브랜치에서 구현한다.
 *
 * TODO:  imageUrl S3 업로드 후 채워진다. 업로드 전에는 빈 문자열.
 */
@Serializable
data class OcrImageUploadPayload(
    val userId: Long,
    val imageUrl: String,
    val ocr: OcrContent,
)

@Serializable
data class OcrContent(
    val rawText: String,
)

const val PLACEHOLDER_USER_ID = 1L

fun SelectedImage.toOcrUploadPayload(
    userId: Long = PLACEHOLDER_USER_ID,
    imageUrl: String = "",
): OcrImageUploadPayload =
    OcrImageUploadPayload(
        userId = userId,
        imageUrl = imageUrl,
        ocr = OcrContent(rawText = ocrText),
    )
