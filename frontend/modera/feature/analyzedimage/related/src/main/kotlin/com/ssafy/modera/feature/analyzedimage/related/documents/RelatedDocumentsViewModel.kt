package com.ssafy.modera.feature.analyzedimage.related.documents

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
    assistedFactory = RelatedDocumentsViewModel.Factory::class,
)
class RelatedDocumentsViewModel @AssistedInject constructor(
    documentRepository: DocumentRepository,
    @Assisted val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<RelatedDocumentsUiState> =
        documentRepository
            .getDocumentsByImageId(imageId)
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        RelatedDocumentsUiState.Loading
                    }

                    is Result.Success -> {
                        if (result.data.isEmpty()) {
                            RelatedDocumentsUiState.Empty
                        } else {
                            RelatedDocumentsUiState.Success(
                                relatedDocuments = result.data,
                            )
                        }
                    }

                    is Result.Error -> {
                        RelatedDocumentsUiState.Error(
                            exception = result.exception
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RelatedDocumentsUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): RelatedDocumentsViewModel
    }
}