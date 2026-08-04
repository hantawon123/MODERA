package com.ssafy.modera.feature.document.documentedit

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface DocumentEditUiState {

    data object Loading : DocumentEditUiState

    data class Success(
        val images: List<AnalyzedImage>,
        val selectedImageIds: Set<Long>,
        val isEditing: Boolean = false,
    ) : DocumentEditUiState

    data class Applying(
        val selectedImages: List<AnalyzedImage>,
    ) : DocumentEditUiState

    data class Error(
        val exception: Throwable,
    ) : DocumentEditUiState
}