package com.ssafy.modera.feature.documentdetail

import com.ssafy.modera.core.model.DocumentDetail

sealed interface DocumentDetailUiState {

    data object Loading : DocumentDetailUiState

    data object Reanalyzing : DocumentDetailUiState
    data class Success(
        val document: DocumentDetail,
    ) : DocumentDetailUiState

    data class Error(
        val exception: Throwable,
    ) : DocumentDetailUiState
}