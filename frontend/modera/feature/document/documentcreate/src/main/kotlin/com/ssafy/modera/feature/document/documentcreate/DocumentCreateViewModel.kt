package com.ssafy.modera.feature.document.documentcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel(
    assistedFactory = DocumentCreateViewModel.Factory::class,
)
class DocumentCreateViewModel @AssistedInject constructor(
    private val analyzedImageRepository: AnalyzedImageRepository,
    private val documentRepository: DocumentRepository,
    @Assisted private val initialImage: AnalyzedImage,
) : ViewModel() {

    private var clientRequestId: String =
        UUID.randomUUID().toString()

    val hasSelectionChanged: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val isCreatingDocument: StateFlow<Boolean>
        field = MutableStateFlow(false)

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
        if (selectedImages.value.any { it.id == analyzedImage.id }) {
            return
        }

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

        hasSelectionChanged.value = true
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
        hasSelectionChanged.value = true
    }

    fun refreshRecommendedImages() {
        val requestedImageIds =
            selectedImages.value.map(AnalyzedImage::id)

        viewModelScope.launch {
            analyzedImageRepository
                .getDocumentRecommendedImages(
                    selectedImageIds = requestedImageIds,
                )
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            DocumentCreateUiState.Loading
                        }

                        is Result.Success -> {
                            hasSelectionChanged.value =
                                selectedImages.value
                                    .map(AnalyzedImage::id) != requestedImageIds

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

    fun createDocument(onCreated: (Long) -> Unit) {
        if (isCreatingDocument.value) return

        val imageIds = selectedImages.value.map {
            it.id
        }

        if (imageIds.isEmpty()) return

        isCreatingDocument.value = true

        viewModelScope.launch {
            documentRepository
                .createDocument(
                    clientRequestId = clientRequestId,
                    imageIds = imageIds,
                )
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> {
                            uiState.value = DocumentCreateUiState.Creating
                        }

                        is Result.Success -> {
                            isCreatingDocument.value = false

                            val createdDocument = result.data

                            clientRequestId =
                                UUID.randomUUID().toString()

                            onCreated(createdDocument.id)
                        }

                        is Result.Error -> {
                            isCreatingDocument.value = false
                            uiState.value =
                                DocumentCreateUiState.Error(
                                    exception = result.exception,
                                )
                        }
                    }
                }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            initialImage: AnalyzedImage,
        ): DocumentCreateViewModel
    }
}