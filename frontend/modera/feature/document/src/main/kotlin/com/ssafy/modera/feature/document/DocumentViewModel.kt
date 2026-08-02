package com.ssafy.modera.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val request = MutableStateFlow(
        DocumentRequest(
            page = INITIAL_PAGE,
            sortType = DocumentSortType.LATEST,
        ),
    )

    private var documents: List<Document> = emptyList()
    private var isLoading = true
    private var isLastPageReached = false
    private var hasResumedOnce = false

    val uiState: StateFlow<DocumentUiState> =
        request.flatMapLatest { request ->
            documentRepository
                .getDocuments(
                    page = request.page,
                    sortType = request.sortType,
                    onLastPageReached = {
                        isLastPageReached = true
                    },
                )
                .asResult()
                .map { result ->
                    when (result) {
                        Result.Loading -> {
                            isLoading = true

                            if (request.page == INITIAL_PAGE) {
                                DocumentUiState.Loading
                            } else {
                                DocumentUiState.Success(
                                    documents = documents,
                                    sortType = request.sortType,
                                )
                            }
                        }

                        is Result.Success -> {
                            isLoading = false

                            documents = if (request.page == INITIAL_PAGE) {
                                result.data
                            } else {
                                (documents + result.data)
                                    .distinctBy(Document::id)
                            }

                            if (documents.isEmpty()) {
                                DocumentUiState.Empty
                            } else {
                                DocumentUiState.Success(
                                    documents = documents,
                                    sortType = request.sortType,
                                )
                            }
                        }

                        is Result.Error -> {
                            isLoading = false

                            if (request.page == INITIAL_PAGE) {
                                DocumentUiState.Error(
                                    exception = result.exception,
                                )
                            } else {
                                DocumentUiState.Success(
                                    documents = documents,
                                    sortType = request.sortType,
                                )
                            }
                        }
                    }
                }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DocumentUiState.Loading,
            )

    fun onScreenResumed() {
        if (!hasResumedOnce) {
            hasResumedOnce = true
            return
        }

        refreshDocuments()
    }

    private fun refreshDocuments() {
        documents = emptyList()
        isLoading = true
        isLastPageReached = false

        request.update { currentRequest ->
            currentRequest.copy(
                page = INITIAL_PAGE,
                refreshVersion = currentRequest.refreshVersion + 1,
            )
        }
    }

    fun updateSortType(
        sortType: DocumentSortType,
    ) {
        if (request.value.sortType == sortType) return

        documents = emptyList()
        isLoading = true
        isLastPageReached = false

        request.value = DocumentRequest(
            page = INITIAL_PAGE,
            sortType = sortType,
        )
    }

    fun loadNextPage() {
        if (isLoading || isLastPageReached) return

        isLoading = true

        request.update { currentRequest ->
            currentRequest.copy(
                page = currentRequest.page + 1,
            )
        }
    }

    private data class DocumentRequest(
        val page: Int,
        val sortType: DocumentSortType,
        val refreshVersion: Long = 0L,
    )

    private companion object {
        const val INITIAL_PAGE = 0
    }
}