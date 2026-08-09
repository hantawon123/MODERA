package com.ssafy.modera.feature.document.documentedit

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface DocumentEditUiState {

    data object Loading : DocumentEditUiState

    data class Success(
        val images: List<AnalyzedImage>,
    ) : DocumentEditUiState

    data class Error(
        val exception: Throwable,
    ) : DocumentEditUiState
}