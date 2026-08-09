package com.ssafy.modera.feature.analyzedimage.related.documents

import com.ssafy.modera.core.model.document.Document

sealed interface RelatedDocumentsUiState {

    data object Loading : RelatedDocumentsUiState

    data class Success(val relatedDocuments: List<Document>) : RelatedDocumentsUiState

    data class Error(val exception: Throwable) : RelatedDocumentsUiState

    data object Empty : RelatedDocumentsUiState
}