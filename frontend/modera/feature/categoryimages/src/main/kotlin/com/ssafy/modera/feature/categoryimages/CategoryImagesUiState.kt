package com.ssafy.modera.feature.categoryimages

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary

sealed interface CategoryImagesUiState {

    data object Loading : CategoryImagesUiState

    data class Success(
        val images: List<AnalyzedImageSummary>,
    ) : CategoryImagesUiState

    data object Error : CategoryImagesUiState
}