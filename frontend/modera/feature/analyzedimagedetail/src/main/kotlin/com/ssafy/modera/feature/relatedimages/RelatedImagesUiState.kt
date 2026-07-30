package com.ssafy.modera.feature.relatedimages

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary

sealed interface RelatedImagesUiState {

    data object Loading : RelatedImagesUiState

    data class Success(val relatedImages: List<AnalyzedImageSummary>) : RelatedImagesUiState

    data class Error(val message: String) : RelatedImagesUiState

    data object Empty : RelatedImagesUiState
}