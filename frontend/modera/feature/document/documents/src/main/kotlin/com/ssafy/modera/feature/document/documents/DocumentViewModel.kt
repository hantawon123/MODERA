package com.ssafy.modera.feature.document.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.model.document.DocumentSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val sortType =
        MutableStateFlow(DocumentSortType.LATEST)

    val uiState: StateFlow<DocumentUiState> =
        sortType
            .flatMapLatest { sortType ->
                documentRepository
                    .getDocuments(
                        sortType = sortType,
                    )
                    .asResult()
                    .map { result ->
                        when (result) {
                            Result.Loading -> {
                                DocumentUiState.Loading
                            }

                            is Result.Success -> {
                                if (result.data.isEmpty()) {
                                    DocumentUiState.Empty
                                } else {
                                    DocumentUiState.Success(
                                        documents = result.data,
                                        sortType = sortType,
                                    )
                                }
                            }

                            is Result.Error -> {
                                DocumentUiState.Error(
                                    exception = result.exception,
                                )
                            }
                        }
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DocumentUiState.Loading,
            )

    fun updateSortType(
        sortType: DocumentSortType,
    ) {
        if (this.sortType.value == sortType) return

        this.sortType.value = sortType
    }
}