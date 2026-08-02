package com.ssafy.modera.feature.favorite

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface FavoritesUiState {

    data object Loading : FavoritesUiState

    data object Error : FavoritesUiState

    data class Success(
        val favorites: List<AnalyzedImage>,
        val isLoadingMore: Boolean = false,
        val hasNextPage: Boolean = false,
    ) : FavoritesUiState
}
