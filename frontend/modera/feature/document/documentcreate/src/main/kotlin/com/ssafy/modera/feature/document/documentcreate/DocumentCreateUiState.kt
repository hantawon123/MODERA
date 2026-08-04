package com.ssafy.modera.feature.document.documentcreate

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface DocumentCreateUiState {

    data object Loading : DocumentCreateUiState

    data class Success(
        val recommendedImages: List<AnalyzedImage>,
    ) : DocumentCreateUiState

    data object Creating : DocumentCreateUiState

    data class Error(
        val exception: Throwable,
    ) : DocumentCreateUiState
}