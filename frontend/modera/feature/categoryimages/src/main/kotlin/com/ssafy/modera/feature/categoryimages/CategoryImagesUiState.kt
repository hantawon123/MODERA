package com.ssafy.modera.feature.categoryimages

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface CategoryImagesUiState {

    data object Loading : CategoryImagesUiState

    data class Success(
        val images: List<AnalyzedImage>,
    ) : CategoryImagesUiState

    data object Error : CategoryImagesUiState
}