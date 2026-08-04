package com.ssafy.modera.feature.document.documentedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(
    assistedFactory = DocumentEditViewModel.Factory::class,
)
class DocumentEditViewModel @AssistedInject constructor(
    documentRepository: DocumentRepository,
    @Assisted documentId: Long,
) : ViewModel() {

    val uiState: StateFlow<DocumentEditUiState> =
        documentRepository
            .getDocumentImages(documentId)
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        DocumentEditUiState.Loading
                    }

                    is Result.Success -> {
                        DocumentEditUiState.Success(
                            images = result.data,
                        )
                    }

                    is Result.Error -> {
                        DocumentEditUiState.Error(
                            exception = result.exception,
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DocumentEditUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            documentId: Long,
        ): DocumentEditViewModel
    }
}