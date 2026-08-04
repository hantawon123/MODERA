package com.ssafy.modera.feature.document.documents

import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType

sealed interface DocumentUiState {

    data object Loading : DocumentUiState

    data class Success(
        val documents: List<Document>,
        val sortType: DocumentSortType = DocumentSortType.LATEST,
    ) : DocumentUiState

    data object Empty : DocumentUiState

    data class Error(
        val exception: Throwable,
    ) : DocumentUiState
}