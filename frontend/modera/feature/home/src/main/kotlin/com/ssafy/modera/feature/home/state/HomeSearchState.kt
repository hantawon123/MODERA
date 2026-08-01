package com.ssafy.modera.feature.home.state

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

data class HomeSearchState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSearchBarFocused: Boolean = false,
    val searchResults: List<AnalyzedImage> = emptyList(),
    val isShowingSearchResults: Boolean = false,
    val isSearchLoading: Boolean = false,
)
