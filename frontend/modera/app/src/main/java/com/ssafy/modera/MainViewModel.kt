package com.ssafy.modera

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.media.ImageTextRecognizer
import com.ssafy.modera.media.OcrImageUploadPayload
import com.ssafy.modera.media.SelectedImage
import com.ssafy.modera.media.toOcrUploadPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class MainUiState(
    val selectedImages: List<SelectedImage> = emptyList(),
    val ocrUploadPayloads: List<OcrImageUploadPayload> = emptyList(),
    val showAnalysisBanner: Boolean = false,
    val analysisImageCount: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var ocrJob: Job? = null

    fun onImagesPicked(images: List<SelectedImage>) {
        if (images.isEmpty()) return

        ocrJob?.cancel()
        _uiState.update {
            it.copy(
                showAnalysisBanner = true,
                analysisImageCount = images.size,
            )
        }

        ocrJob = viewModelScope.launch {
            val processed = withContext(Dispatchers.IO) {
                ImageTextRecognizer().use { ocr ->
                    ocr.recognizeAll(appContext, images)
                }
            }
            val payloads = processed.map { it.toOcrUploadPayload() }
            logOcrResults(processed)

            _uiState.update {
                it.copy(
                    selectedImages = processed,
                    ocrUploadPayloads = payloads,
                )
            }

            delay(2.seconds)
            dismissAnalysisBanner()
        }
    }

    fun dismissAnalysisBanner() {
        _uiState.update { it.copy(showAnalysisBanner = false) }
    }

    private fun logOcrResults(images: List<SelectedImage>) {
        Log.d(OCR_LOG_TAG, "OCR 완료: ${images.size}장")
        images.forEachIndexed { index, image ->
            Log.d(
                OCR_LOG_TAG,
                "[$index] file=${image.originalFileName}, size=${image.fileSizeBytes}B\n" +
                    "rawText=\n${image.ocrText.ifBlank { "(empty)" }}",
            )
        }
    }

    private companion object {
        const val OCR_LOG_TAG = "MODERA_OCR"
    }
}
