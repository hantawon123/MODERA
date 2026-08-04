package com.ssafy.modera.feature.documentedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel(
    assistedFactory = DocumentEditViewModel.Factory::class,
)
class DocumentEditViewModel @AssistedInject constructor(
    private val documentRepository: DocumentRepository,
    @Assisted private val documentId: Long,
) : ViewModel() {

    private var clientRequestId: String =
        UUID.randomUUID().toString()

    val uiState: StateFlow<DocumentEditUiState>
        field = MutableStateFlow<DocumentEditUiState>(
            DocumentEditUiState.Loading,
        )

    init {
        loadDocumentImages()
    }

    private fun loadDocumentImages() {
        viewModelScope.launch {
            documentRepository
                .getDocumentImages(documentId)
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            DocumentEditUiState.Loading
                        }

                        is Result.Success -> {
                            DocumentEditUiState.Success(
                                images = result.data,
                                selectedImageIds = result.data
                                    .map(AnalyzedImage::id)
                                    .toSet(),
                            )
                        }

                        is Result.Error -> {
                            DocumentEditUiState.Error(
                                exception = result.exception,
                            )
                        }
                    }
                }
        }
    }

    fun startEditing() {
        val currentState =
            uiState.value as? DocumentEditUiState.Success
                ?: return

        uiState.value = currentState.copy(
            isEditing = true,
        )
    }

    fun toggleImageSelection(imageId: Long) {
        val currentState =
            uiState.value as? DocumentEditUiState.Success
                ?: return

        if (!currentState.isEditing) {
            return
        }

        val selectedImageIds =
            currentState.selectedImageIds.toMutableSet().apply {
                if (!add(imageId)) {
                    remove(imageId)
                }
            }

        uiState.value = currentState.copy(
            selectedImageIds = selectedImageIds,
        )
    }

    fun createDocument(
        onCreated: (Long) -> Unit,
    ) {
        val currentState =
            uiState.value as? DocumentEditUiState.Success
                ?: return

        val selectedImages = currentState.images.filter { image ->
            image.id in currentState.selectedImageIds
        }

        if (selectedImages.isEmpty()) {
            return
        }

        val imageIds = selectedImages.map(AnalyzedImage::id)

        uiState.value = DocumentEditUiState.Applying(
            selectedImages = selectedImages,
        )

        viewModelScope.launch {
            documentRepository
                .createDocument(
                    clientRequestId = clientRequestId,
                    imageIds = imageIds,
                )
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> Unit

                        is Result.Success -> {
                            clientRequestId =
                                UUID.randomUUID().toString()

                            onCreated(result.data.id)
                        }

                        is Result.Error -> {
                            uiState.value =
                                DocumentEditUiState.Error(
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
            documentId: Long,
        ): DocumentEditViewModel
    }
}