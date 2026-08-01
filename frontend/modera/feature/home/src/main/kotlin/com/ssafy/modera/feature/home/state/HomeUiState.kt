package com.ssafy.modera.feature.home.state

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.category.Category

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val categories: List<Category>,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val recentSearchTerms: List<String> = emptyList(),
        val searchResults: List<AnalyzedImage> = emptyList(),
        val isShowingSearchResults: Boolean = false,
        val isSearchLoading: Boolean = false,
    ) : HomeUiState

    data class Error(
        val exception: Throwable,
    ) : HomeUiState
}