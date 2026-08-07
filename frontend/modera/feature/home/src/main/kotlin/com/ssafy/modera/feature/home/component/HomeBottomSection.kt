package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.feature.home.HomeScreenDefaults
import com.ssafy.modera.feature.home.state.HomeUiState

@Composable
internal fun HomeBottomSection(
    uiState: HomeUiState.Success,
    categoryContentAlpha: Float,
    onCategoryClick: (Category) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    onRecentSearchClearAll: () -> Unit,
    onSearchResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (categoryContentAlpha > 0f) {
            HomeCategoryGrid(
                categories = uiState.categories,
                onCategoryClick = onCategoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = categoryContentAlpha },
            )
        }

        if (uiState.isSearchActive) {
            HomeSearchContent(
                uiState = uiState,
                onRecentSearchClick = onRecentSearchClick,
                onRecentSearchDelete = onRecentSearchDelete,
                onRecentSearchClearAll = onRecentSearchClearAll,
                onSearchResultClick = onSearchResultClick,
                modifier = Modifier.padding(horizontal = HomeScreenDefaults.HorizontalPadding),
            )
        }
    }
}

@Composable
private fun HomeSearchContent(
    uiState: HomeUiState.Success,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    onRecentSearchClearAll: () -> Unit,
    onSearchResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isShowingSearchResults -> {
            SearchResultSection(
                searchResults = uiState.searchResults,
                isLoading = uiState.isSearchLoading,
                onSearchResultClick = onSearchResultClick,
                modifier = modifier.fillMaxSize(),
            )
        }

        else -> {
            RecentSearchSection(
                recentSearchQueries = uiState.recentSearchQueries,
                onRecentSearchClick = onRecentSearchClick,
                onRecentSearchDelete = onRecentSearchDelete,
                onRecentSearchClearAll = onRecentSearchClearAll,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}
