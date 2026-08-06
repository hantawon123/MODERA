package com.ssafy.modera.feature.analyzedimage.related.images

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface RelatedImagesUiState {

    data object Loading : RelatedImagesUiState

    data class Success(val relatedImages: List<AnalyzedImage>) : RelatedImagesUiState

    data class Error(val exception: Throwable) : RelatedImagesUiState

    data object Empty : RelatedImagesUiState
}