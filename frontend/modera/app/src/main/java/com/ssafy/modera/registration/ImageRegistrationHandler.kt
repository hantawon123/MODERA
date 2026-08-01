package com.ssafy.modera.registration

import android.content.Context
import com.ssafy.modera.core.data.repository.ImageRepository
import com.ssafy.modera.core.model.image.FailedImage
import com.ssafy.modera.core.model.image.RegisteredImage
import com.ssafy.modera.core.model.image.SelectedImage
import com.ssafy.modera.media.ImageTextRecognizer
import com.ssafy.modera.media.PresignedUrlUploader
import com.ssafy.modera.media.toRegisterImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImageRegistrationHandler @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val imageRepository: ImageRepository,
    private val presignedUrlUploader: PresignedUrlUploader,
) {

    suspend fun registerImage(
        ocr: ImageTextRecognizer,
        image: SelectedImage,
        index: Int,
    ): ImageRegistrationOutcome {
        val processed = recognizeOcr(ocr, image, index)
        val registerImage = buildRegisterImage(processed, index)
            .getOrElse { error ->
                return ImageRegistrationOutcome(
                    processedImage = processed,
                    failed = listOf(
                        FailedImage(
                            clientRequestId = "local-$index",
                            fileName = processed.originalFileName,
                            reason = error.message ?: "CONTENT_HASH_FAILED",
                        ),
                    ),
                )
            }

        val registerResult = runCatching {
            imageRepository.registerImages(listOf(registerImage)).first()
        }.getOrElse { error ->
            val failed = FailedImage(
                clientRequestId = registerImage.clientRequestId,
                fileName = registerImage.fileName,
                reason = error.message ?: "REGISTER_REQUEST_FAILED",
            )
            ImageRegisterLogger.logRegisterFailure(index, failed, error)
            return ImageRegistrationOutcome(
                processedImage = processed,
                failed = listOf(failed),
            )
        }

        ImageRegisterLogger.logRegisterResponse(
            index = index,
            clientRequestId = registerImage.clientRequestId,
            result = registerResult,
        )

        val uploadOutcome = uploadRegisteredImages(
            index = index,
            image = processed,
            registeredItems = registerResult.registered,
        )

        return ImageRegistrationOutcome(
            processedImage = processed,
            registered = uploadOutcome.registered,
            duplicated = registerResult.duplicated,
            failed = registerResult.failed + uploadOutcome.failed,
        )
    }

    private suspend fun recognizeOcr(
        ocr: ImageTextRecognizer,
        image: SelectedImage,
        index: Int,
    ): SelectedImage {
        val processed = withContext(Dispatchers.IO) {
            val ocrText = runCatching { ocr.recognize(appContext, image) }
                .getOrDefault("")
            image.copy(ocrText = ocrText)
        }
        ImageRegisterLogger.logOcrResult(index, processed)
        return processed
    }

    private suspend fun buildRegisterImage(
        processed: SelectedImage,
        index: Int,
    ) = withContext(Dispatchers.IO) {
        runCatching { processed.toRegisterImage(appContext) }
    }.onFailure { error ->
        ImageRegisterLogger.logRegisterImageBuildFailure(
            index = index,
            fileName = processed.originalFileName,
            error = error,
        )
    }

    private suspend fun uploadRegisteredImages(
        index: Int,
        image: SelectedImage,
        registeredItems: List<RegisteredImage>,
    ): UploadOutcome {
        if (registeredItems.isEmpty()) {
            return UploadOutcome()
        }

        val registered = mutableListOf<RegisteredImage>()
        val failed = mutableListOf<FailedImage>()

        registeredItems.forEach { item ->
            runCatching {
                presignedUrlUploader.upload(
                    localUri = image.uri,
                    uploadUrl = item.uploadUrl,
                )
            }.onSuccess {
                registered += item
                ImageRegisterLogger.logUploadSuccess(index, item)
            }.onFailure { error ->
                failed += FailedImage(
                    clientRequestId = item.clientRequestId,
                    fileName = item.fileName,
                    reason = error.message ?: "UPLOAD_FAILED",
                )
                ImageRegisterLogger.logUploadFailure(index, item, error)
            }
        }

        return UploadOutcome(
            registered = registered,
            failed = failed,
        )
    }

    private data class UploadOutcome(
        val registered: List<RegisteredImage> = emptyList(),
        val failed: List<FailedImage> = emptyList(),
    )
}
