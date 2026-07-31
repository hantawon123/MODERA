package com.ssafy.modera.feature.documentcreate

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface DocumentCreateUiState {

    data object Loading : DocumentCreateUiState

    data class Success(
        val selectedImages: List<AnalyzedImage>,
        val recommendedImages: List<AnalyzedImage>,
        val isRecommendationLoading: Boolean = false,
        val isCreating: Boolean = false,
    ) : DocumentCreateUiState {

        val canCreateDocument: Boolean
            get() = selectedImages.size > 1 && !isCreating
    }

    data class Error(
        val exception: Throwable,
    ) : DocumentCreateUiState
}