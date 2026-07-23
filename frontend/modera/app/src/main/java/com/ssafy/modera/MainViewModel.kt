package com.ssafy.modera

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.ImageRepository
import com.ssafy.modera.core.model.image.DuplicatedImage
import com.ssafy.modera.core.model.image.FailedImage
import com.ssafy.modera.core.model.image.RegisterImage
import com.ssafy.modera.core.model.image.RegisterImagesResult
import com.ssafy.modera.core.model.image.RegisteredImage
import com.ssafy.modera.core.model.image.SelectedImage
import com.ssafy.modera.media.ImageTextRecognizer
import com.ssafy.modera.media.PresignedUrlUploader
import com.ssafy.modera.media.toRegisterImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class MainUiState(
    val selectedImages: List<SelectedImage> = emptyList(),
    val registeredImages: List<RegisteredImage> = emptyList(),
    val duplicatedImages: List<DuplicatedImage> = emptyList(),
    val failedImages: List<FailedImage> = emptyList(),
    val showAnalysisBanner: Boolean = false,
    val analysisImageCount: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val imageRepository: ImageRepository,
    private val presignedUrlUploader: PresignedUrlUploader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var registerJob: Job? = null

    fun onImagesPicked(images: List<SelectedImage>) {
        if (images.isEmpty()) return

        registerJob?.cancel()
        _uiState.update {
            it.copy(
                selectedImages = emptyList(),
                registeredImages = emptyList(),
                duplicatedImages = emptyList(),
                failedImages = emptyList(),
                showAnalysisBanner = true,
                analysisImageCount = images.size,
            )
        }

        registerJob = viewModelScope.launch {
            Log.d(REGISTER_LOG_TAG, "등록 시작: ${images.size}장")

            ImageTextRecognizer().use { ocr ->
                images.forEachIndexed { index, image ->
                    processSingleImage(ocr, image, index)
                }
            }

            notifyRegisterSummary(totalCount = images.size)

            delay(2.seconds)
            dismissAnalysisBanner()
        }
    }

    private suspend fun processSingleImage(
        ocr: ImageTextRecognizer,
        image: SelectedImage,
        index: Int,
    ) {
        val processed = withContext(Dispatchers.IO) {
            val ocrText = runCatching { ocr.recognize(appContext, image) }
                .getOrDefault("")
            image.copy(ocrText = ocrText)
        }
        logOcrResult(index, processed)

        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages + processed)
        }

        val registerImageResult = withContext(Dispatchers.IO) {
            runCatching { processed.toRegisterImage(appContext) }
        }
        val registerImage = registerImageResult.getOrElse { error ->
            appendFailed(
                FailedImage(
                    clientRequestId = "local-$index",
                    fileName = processed.originalFileName,
                    reason = error.message ?: "CONTENT_HASH_FAILED",
                ),
            )
            Log.e(
                REGISTER_LOG_TAG,
                "[$index] RegisterImage 생성 실패: ${error.message}",
                error,
            )
            return
        }

        logRegisterRequest(index, registerImage)

        val registerResult = runCatching {
            imageRepository.registerImages(listOf(registerImage)).first()
        }.getOrElse { error ->
            val failed = FailedImage(
                clientRequestId = registerImage.clientRequestId,
                fileName = registerImage.fileName,
                reason = error.message ?: "REGISTER_REQUEST_FAILED",
            )
            logRegisterFailure(index, failed, error)
            appendFailed(failed)
            return
        }

        logRegisterResponse(index, registerImage.clientRequestId, registerResult)
        _uiState.update { state ->
            state.copy(
                duplicatedImages = state.duplicatedImages + registerResult.duplicated,
                failedImages = state.failedImages + registerResult.failed,
            )
        }

        registerResult.registered.forEach { registered ->
            uploadAndNotifyComplete(index, processed, registered)
        }
    }

    private suspend fun uploadAndNotifyComplete(
        index: Int,
        localImage: SelectedImage,
        registered: RegisteredImage,
    ) {
        runCatching {
            Log.d(
                REGISTER_LOG_TAG,
                "[$index] 스토리지 업로드 시작 imageId=${registered.imageId}, url=${registered.uploadUrl}",
            )
            presignedUrlUploader.upload(
                localUri = localImage.uri,
                uploadUrl = registered.uploadUrl,
            )
            Log.d(
                REGISTER_LOG_TAG,
                "[$index] 스토리지 업로드 성공 imageId=${registered.imageId}",
            )

            val complete = imageRepository.notifyUploadComplete(registered.imageId).first()
            Log.d(
                REGISTER_LOG_TAG,
                "[$index] upload-complete 성공 imageId=${complete.imageId}, " +
                    "uploadCompleted=${complete.uploadCompleted}, uploadedAt=${complete.uploadedAt}",
            )
            registered
        }.onSuccess { uploaded ->
            _uiState.update { state ->
                state.copy(registeredImages = state.registeredImages + uploaded)
            }
        }.onFailure { error ->
            val failed = FailedImage(
                clientRequestId = registered.clientRequestId,
                fileName = registered.fileName,
                reason = error.message ?: "UPLOAD_OR_COMPLETE_FAILED",
            )
            Log.e(
                REGISTER_LOG_TAG,
                "[$index] 업로드완료알림 실패 imageId=${registered.imageId}, reason=${failed.reason}",
                error,
            )
            appendFailed(failed)
        }
    }

    private suspend fun notifyRegisterSummary(totalCount: Int) {
        val state = _uiState.value
        val registeredCount = state.registeredImages.size
        val duplicatedCount = state.duplicatedImages.size
        val failedCount = state.failedImages.size

        Log.d(
            REGISTER_LOG_TAG,
            "등록 완료 summary: total=$totalCount, " +
                "registered=$registeredCount, duplicated=$duplicatedCount, failed=$failedCount",
        )

        if (totalCount <= 0) return

        _snackbarMessage.emit(
            "성공 ${registeredCount}장, 중복 ${duplicatedCount}장, 실패 ${failedCount}장",
        )
    }

    private fun appendFailed(failed: FailedImage) {
        _uiState.update { state ->
            state.copy(failedImages = state.failedImages + failed)
        }
    }

    fun dismissAnalysisBanner() {
        _uiState.update { it.copy(showAnalysisBanner = false) }
    }

    private fun logOcrResult(index: Int, image: SelectedImage) {
        Log.d(
            OCR_LOG_TAG,
            "[$index] OCR file=${image.originalFileName}, size=${image.fileSizeBytes}B\n" +
                "rawText=\n${image.ocrText.ifBlank { "(empty)" }}",
        )
    }

    private fun logRegisterRequest(index: Int, image: RegisterImage) {
        Log.d(
            REGISTER_LOG_TAG,
            "[$index] API 요청 POST /api/v1/images/upload\n" +
                "clientRequestId=${image.clientRequestId}\n" +
                "fileName=${image.fileName}\n" +
                "contentHash=${image.contentHash}\n" +
                "fileSize=${image.fileSize}\n" +
                "ocr.rawText=${image.ocr.rawText.take(120)}" +
                if (image.ocr.rawText.length > 120) "..." else "",
        )
    }

    private fun logRegisterResponse(
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
            Log.d(
                REGISTER_LOG_TAG,
                "[$index] registered imageId=${item.imageId}, " +
                    "uploadExpiresIn=${item.uploadExpiresIn}, uploadUrl=${item.uploadUrl}",
            )
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

    private fun logRegisterFailure(index: Int, failed: FailedImage, error: Throwable) {
        Log.e(
            REGISTER_LOG_TAG,
            "[$index] API 호출 실패 clientRequestId=${failed.clientRequestId}, " +
                "file=${failed.fileName}, reason=${failed.reason}",
            error,
        )
    }

    private companion object {
        const val OCR_LOG_TAG = "MODERA_OCR"
        const val REGISTER_LOG_TAG = "MODERA_REGISTER_OKHTTP"
    }
}
