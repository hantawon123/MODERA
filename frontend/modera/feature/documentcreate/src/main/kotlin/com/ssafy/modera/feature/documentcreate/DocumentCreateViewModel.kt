package com.ssafy.modera.feature.documentcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(
    assistedFactory = DocumentCreateViewModel.Factory::class,
)
class DocumentCreateViewModel @AssistedInject constructor(
    private val analyzedImageRepository: AnalyzedImageRepository,
    @Assisted private val initialImage: AnalyzedImage,
) : ViewModel() {

    val selectedImages: StateFlow<List<AnalyzedImage>>
        field = MutableStateFlow(
            listOf(initialImage),
        )

    val uiState: StateFlow<DocumentCreateUiState>
        field = MutableStateFlow<DocumentCreateUiState>(
            DocumentCreateUiState.Loading,
        )

    init {
        refreshRecommendedImages()
    }

    fun addSelectedImage(
        analyzedImage: AnalyzedImage,
    ) {
        selectedImages.update { images ->
            if (images.any { it.id == analyzedImage.id }) {
                images
            } else {
                images + analyzedImage
            }
        }

        uiState.update { state ->
            if (state is DocumentCreateUiState.Success) {
                state.copy(
                    recommendedImages =
                        state.recommendedImages.filterNot {
                            it.id == analyzedImage.id
                        },
                )
            } else {
                state
            }
        }
    }

    fun removeSelectedImage(
        analyzedImageId: Long,
    ) {
        val removedImage = selectedImages.value
            .firstOrNull { it.id == analyzedImageId }
            ?: return

        selectedImages.update { images ->
            images.filterNot {
                it.id == analyzedImageId
            }
        }

        uiState.update { state ->
            if (state is DocumentCreateUiState.Success) {
                state.copy(
                    recommendedImages =
                        state.recommendedImages.filterNot {
                            it.id == analyzedImageId
                        } + removedImage
                )
            } else {
                state
            }
        }
    }

    fun refreshRecommendedImages() {
        viewModelScope.launch {
            analyzedImageRepository
                .getDocumentRecommendedImages(
                    selectedImageIds = selectedImages.value.map {
                        it.id
                    },
                )
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            DocumentCreateUiState.Loading
                        }

                        is Result.Success -> {
                            DocumentCreateUiState.Success(
                                recommendedImages = result.data,
                            )
                        }

                        is Result.Error -> {
                            DocumentCreateUiState.Error(
                                exception = result.exception,
                            )
                        }
                    }
                }
        }
    }

    fun createDocument() {
        val successState =
            uiState.value as? DocumentCreateUiState.Success
                ?: return

        if (selectedImages.value.size <= 1) {
            return
        }

        val selectedImageIds =
            selectedImages.value.map(AnalyzedImage::id)

        viewModelScope.launch {
            uiState.value = DocumentCreateUiState.Creating

            // TODO 문서 생성 API 연결 시 selectedImageIds 전달
            delay(3_000L.milliseconds)

            uiState.value = successState
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            initialImage: AnalyzedImage,
        ): DocumentCreateViewModel
    }
}