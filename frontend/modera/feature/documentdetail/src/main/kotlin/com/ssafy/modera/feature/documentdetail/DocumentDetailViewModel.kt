package com.ssafy.modera.feature.documentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel(
    assistedFactory = DocumentDetailViewModel.Factory::class,
)
class DocumentDetailViewModel @AssistedInject constructor(
    private val documentRepository: DocumentRepository,
    @Assisted private val documentId: Long,
) : ViewModel() {

    val uiState: StateFlow<DocumentDetailUiState>
        field = MutableStateFlow<DocumentDetailUiState>(
            DocumentDetailUiState.Loading,
        )

    init {
        loadDocumentDetail()
    }

    private fun loadDocumentDetail() {
        viewModelScope.launch {
            documentRepository
                .getDocumentDetail(
                    documentId = documentId,
                )
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            DocumentDetailUiState.Loading
                        }

                        is Result.Success -> {
                            DocumentDetailUiState.Success(
                                document = result.data,
                            )
                        }

                        is Result.Error -> {
                            DocumentDetailUiState.Error(
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
        ): DocumentDetailViewModel
    }
}