package com.ssafy.modera.registration

import android.util.Log
import com.ssafy.modera.core.model.image.FailedImage
import com.ssafy.modera.core.model.image.RegisterImage
import com.ssafy.modera.core.model.image.RegisterImagesResult
import com.ssafy.modera.core.model.image.RegisteredImage
import com.ssafy.modera.core.model.image.SelectedImage

internal object ImageRegisterLogger {

    private const val OCR_LOG_TAG = "MODERA_OCR"
    private const val REGISTER_LOG_TAG = "MODERA_REGISTER_OKHTTP"

    fun logRegistrationStart(imageCount: Int) {
        Log.d(REGISTER_LOG_TAG, "등록 시작: ${imageCount}장")
    }

    fun logRegistrationSummary(
        totalCount: Int,
        registeredCount: Int,
        duplicatedCount: Int,
        failedCount: Int,
    ) {
        Log.d(
            REGISTER_LOG_TAG,
            "등록 완료 summary: total=$totalCount, " +
                "registered=$registeredCount, duplicated=$duplicatedCount, failed=$failedCount",
        )
    }

    fun logOcrResult(index: Int, image: SelectedImage) {
        Log.d(
            OCR_LOG_TAG,
            "[$index] OCR file=${image.originalFileName}, size=${image.fileSizeBytes}B\n" +
                "rawText=\n${image.ocrText.ifBlank { "(empty)" }}",
        )
    }

    fun logRegisterResponse(
        index: Int,
        clientRequestId: String,
        result: RegisterImagesResult,
    ) {
        Log.d(
            REGISTER_LOG_TAG,
            "[$index] API 응답 clientRequestId=$clientRequestId " +
                "registered=${result.registered.size}, " +
                "duplicated=${result.duplicated.size}, " +
                "failed=${result.failed.size}",
        )
        result.registered.forEach { item ->
            logRegisteredItem(index, item)
        }
        result.duplicated.forEach { item ->
            Log.d(
                REGISTER_LOG_TAG,
                "[$index] duplicated existingImageId=${item.existingImageId}, file=${item.fileName}",
            )
        }
        result.failed.forEach { item ->
            Log.e(
                REGISTER_LOG_TAG,
                "[$index] failed reason=${item.reason}, file=${item.fileName}",
            )
        }
    }

    fun logRegisterFailure(index: Int, failed: FailedImage, error: Throwable) {
        Log.e(
            REGISTER_LOG_TAG,
            "[$index] API 호출 실패 clientRequestId=${failed.clientRequestId}, " +
                "file=${failed.fileName}, reason=${failed.reason}",
            error,
        )
    }

    fun logRegisterImageBuildFailure(index: Int, fileName: String, error: Throwable) {
        Log.e(
            REGISTER_LOG_TAG,
            "[$index] RegisterImage 생성 실패: file=$fileName, reason=${error.message}",
            error,
        )
    }

    fun logUploadSuccess(index: Int, registered: RegisteredImage) {
        Log.d(
            REGISTER_LOG_TAG,
            "[$index] S3 업로드 성공 imageId=${registered.imageId}, file=${registered.fileName}",
        )
    }

    fun logUploadFailure(index: Int, registered: RegisteredImage, error: Throwable) {
        Log.e(
            REGISTER_LOG_TAG,
            "[$index] S3 업로드 실패 imageId=${registered.imageId}, file=${registered.fileName}",
            error,
        )
    }

    private fun logRegisteredItem(index: Int, item: RegisteredImage) {
        Log.d(
            REGISTER_LOG_TAG,
            "[$index] registered imageId=${item.imageId}, " +
                "uploadExpiresIn=${item.uploadExpiresIn}, uploadUrl=${item.uploadUrl}",
        )
    }
}
