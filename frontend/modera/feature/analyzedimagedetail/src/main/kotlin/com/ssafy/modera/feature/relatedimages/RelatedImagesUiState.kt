package com.ssafy.modera.feature.relatedimages

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface RelatedImagesUiState {

    data object Loading : RelatedImagesUiState

    data class Success(val relatedImages: List<AnalyzedImage>) : RelatedImagesUiState

    data class Error(val message: String) : RelatedImagesUiState

    data object Empty : RelatedImagesUiState
}