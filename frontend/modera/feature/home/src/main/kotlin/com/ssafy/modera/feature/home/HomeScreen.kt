package com.ssafy.modera.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.core.util.statusBarTopPadding
import com.ssafy.modera.feature.home.component.HomeBottomSection
import com.ssafy.modera.feature.home.component.HomeSearchBarSection
import com.ssafy.modera.feature.home.component.HomeSearchFocusEffect
import com.ssafy.modera.feature.home.component.HomeUpperSection
import com.ssafy.modera.feature.home.component.rememberHomeSearchLayoutState
import com.ssafy.modera.feature.home.state.HomeUiState

@Composable
fun HomeRoute(
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationPermissionEffect()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            LoadingScreen(
                modifier = modifier,
            )
        }

        is HomeUiState.Success -> {
            HomeSuccessScreen(
                uiState = state,
                onCalendarClick = onCalendarClick,
                onSettingsClick = onSettingsClick,
                onCategoryClick = { category ->
                    onCategoryClick(category)
                    viewModel.clearNewCategoryFlag(category.id)
                },
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onSearchBarFocusChange = viewModel::onSearchBarFocusChanged,
                onSearchSubmit = viewModel::submitSearch,
                onRecentSearchClick = viewModel::selectRecentSearchQuery,
                onRecentSearchDelete = viewModel::removeRecentSearchQuery,
                onRecentSearchClearAll = viewModel::clearRecentSearchQueries,
                onSearchDeactivate = viewModel::deactivateSearch,
                onSearchResultClick = onSearchResultClick,
                modifier = modifier,
            )
        }

        is HomeUiState.Error -> {
            HomeErrorScreen(
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun HomeSuccessScreen(
    uiState: HomeUiState.Success,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchBarFocusChange: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    onRecentSearchClearAll: () -> Unit,
    onSearchDeactivate: () -> Unit,
    onSearchResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val searchLayoutState = rememberHomeSearchLayoutState(uiState.isSearchActive)

    HomeSearchFocusEffect(
        isSearchActive = uiState.isSearchActive,
        isShowingSearchResults = uiState.isShowingSearchResults,
        searchFocusRequester = searchFocusRequester,
        onSearchDeactivate = {
            onSearchDeactivate()
            focusManager.clearFocus()
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .statusBarTopPadding()
            .padding(horizontal = HomeScreenDefaults.HorizontalPadding)
            .then(searchLayoutState.screenHeightModifier),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(searchLayoutState.upperWeight)
                    .then(
                        if (uiState.isSearchActive) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    focusManager.clearFocus()
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                HomeUpperSection(
                    upperContentAlpha = searchLayoutState.upperContentAlpha,
                    onCalendarClick = onCalendarClick,
                    onSettingsClick = onSettingsClick,
                )
            }

            HomeSearchBarSection(
                query = uiState.searchQuery,
                circleExtraOffsetPx = searchLayoutState.circleExtraOffsetPx,
                searchFocusRequester = searchFocusRequester,
                positionModifier = searchLayoutState.searchBarPositionModifier,
                onQueryChange = onSearchQueryChange,
                onFocusChange = onSearchBarFocusChange,
                onSearch = {
                    if (uiState.searchQuery.trim().isNotEmpty()) {
                        focusManager.clearFocus()
                        onSearchSubmit()
                    }
                },
            )

            HomeBottomSection(
                uiState = uiState,
                categoryContentAlpha = searchLayoutState.categoryContentAlpha,
                onCategoryClick = onCategoryClick,
                onRecentSearchClick = onRecentSearchClick,
                onRecentSearchDelete = onRecentSearchDelete,
                onRecentSearchClearAll = onRecentSearchClearAll,
                onSearchResultClick = onSearchResultClick,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (uiState.isSearchActive) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    focusManager.clearFocus()
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun HomeErrorScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray50)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_error_message),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray700,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(
    name = "Home Screen",
    showBackground = true,
)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeScreenPreviewParameterProvider::class)
    previewData: HomeScreenPreviewData,
) {
    ModeraTheme {
        HomeSuccessScreen(
            uiState = HomeUiState.Success(
                categories = previewData.categories,
            ),
            onCalendarClick = {},
            onSettingsClick = {},
            onCategoryClick = {},
            onSearchQueryChange = {},
            onSearchBarFocusChange = {},
            onSearchSubmit = {},
            onRecentSearchClick = {},
            onRecentSearchDelete = {},
            onRecentSearchClearAll = {},
            onSearchDeactivate = {},
            onSearchResultClick = {},
        )
    }
}
