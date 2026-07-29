package com.ssafy.modera.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.item.ModeraSearchBar
import com.ssafy.modera.core.component.item.SearchBarMode
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.home.component.CategoryItem
import com.ssafy.modera.feature.home.component.HomeHeroSection
import com.ssafy.modera.feature.home.component.HomeTopActions
import com.ssafy.modera.feature.home.component.RecentSearchSection
import com.ssafy.modera.feature.home.component.SearchResultSection

@Composable
fun HomeScreen(
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            LoadingScreen(
                modifier = modifier,
            )
        }

        is HomeUiState.Success -> {
            HomeScreen(
                uiState = state,
                onCalendarClick = onCalendarClick,
                onSettingsClick = onSettingsClick,
                onCategoryClick = onCategoryClick,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onSearchBarFocusChange = viewModel::onSearchBarFocusChanged,
                onSearchSubmit = viewModel::submitSearch,
                onRecentSearchClick = viewModel::selectRecentSearch,
                onRecentSearchDelete = viewModel::removeRecentSearchTerm,
                onSearchDeactivate = viewModel::deactivateSearch,
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
fun HomeScreen(
    uiState: HomeUiState.Success,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchBarFocusChange: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    onSearchDeactivate: () -> Unit,
    onSearchResultClick: (SearchMaterialResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val dismissInteractionSource = remember { MutableInteractionSource() }
    var wasSearchActive by remember { mutableStateOf(false) }

    val upperWeight by animateFloatAsState(
        targetValue = if (uiState.isSearchActive) {
            HomeScreenDefaults.CollapsedUpperWeight
        } else {
            HomeScreenDefaults.ExpandedUpperWeight
        },
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.LayoutAnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchUpperWeight",
    )
    val upperContentAlpha by animateFloatAsState(
        targetValue = if (uiState.isSearchActive) 0f else 1f,
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.ContentFadeDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchUpperAlpha",
    )
    val categoryContentAlpha by animateFloatAsState(
        targetValue = if (uiState.isSearchActive) 0f else 1f,
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.ContentFadeDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchCategoryAlpha",
    )
    val searchActiveTopSpacing by animateDpAsState(
        targetValue = if (uiState.isSearchActive) {
            HomeScreenDefaults.SearchActiveTopSpacing
        } else {
            0.dp
        },
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.LayoutAnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchActiveTopSpacing",
    )

    val statusBarTopPadding = rememberRawStatusBarTopPadding()

    fun dismissSearch() {
        onSearchDeactivate()
        focusManager.clearFocus()
    }

    fun submitSearch() {
        if (uiState.searchQuery.trim().isEmpty()) {
            return
        }
        focusManager.clearFocus()
        onSearchSubmit()
    }

    BackHandler(enabled = uiState.isSearchActive) {
        dismissSearch()
    }

    LaunchedEffect(uiState.isSearchActive) {
        when {
            uiState.isSearchActive && !wasSearchActive && !uiState.isShowingSearchResults -> {
                searchFocusRequester.requestFocus()
            }

            !uiState.isSearchActive && wasSearchActive -> {
                focusManager.clearFocus()
            }
        }
        wasSearchActive = uiState.isSearchActive
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(top = statusBarTopPadding)
            .padding(horizontal = HomeScreenDefaults.HorizontalPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(upperWeight)
                .graphicsLayer { alpha = upperContentAlpha }
                .then(
                    if (uiState.isSearchActive) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HomeTopActions(
                    onCalendarClick = onCalendarClick,
                    onSettingsClick = onSettingsClick,
                )
                Spacer(Modifier.weight(1f))
                Spacer(modifier = Modifier.height(searchActiveTopSpacing))
                HomeHeroSection()
            }
        }

        ModeraSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = stringResource(R.string.home_search_placeholder),
            mode = SearchBarMode.Ai,
            focusRequester = searchFocusRequester,
            onFocusChanged = onSearchBarFocusChange,
            onSearch = ::submitSearch,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    enabled = uiState.isSearchActive &&
                        uiState.searchQuery.isEmpty() &&
                        !uiState.isShowingSearchResults,
                    interactionSource = dismissInteractionSource,
                    indication = null,
                ) {
                    focusManager.clearFocus()
                    onSearchDeactivate()
                },
        ) {
            if (categoryContentAlpha > 0f) {
                HomeCategoryGrid(
                    categories = uiState.categories,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 24.dp)
                        .graphicsLayer { alpha = categoryContentAlpha },
                )
            }

            if (uiState.isSearchActive) {
                if (uiState.isShowingSearchResults) {
                    SearchResultSection(
                        searchResults = uiState.searchResults,
                        isLoading = uiState.isSearchLoading,
                        onSearchResultClick = onSearchResultClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    RecentSearchSection(
                        recentSearchTerms = uiState.recentSearchTerms,
                        onRecentSearchClick = onRecentSearchClick,
                        onRecentSearchDelete = onRecentSearchDelete,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayCategories = categories.take(HomeScreenDefaults.MaxCategoryCount)
    val rows = displayCategories.chunked(HomeScreenDefaults.CategoryGridColumns)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HomeScreenDefaults.CategoryGridSpacing),
    ) {
        rows.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HomeScreenDefaults.CategoryGridSpacing),
            ) {
                repeat(HomeScreenDefaults.CategoryGridColumns) { columnIndex ->
                    val category = rowCategories.getOrNull(columnIndex)
                    if (category != null) {
                        CategoryItem(
                            title = category.title,
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
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

@Composable
private fun rememberRawStatusBarTopPadding(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var topPx by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        fun readTopPx() {
            topPx = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())
                ?.top
                ?: 0
        }

        readTopPx()

        val callback = object : WindowInsetsAnimationCompat.Callback(
            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
        ) {
            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                topPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                readTopPx()
            }
        }
        ViewCompat.setWindowInsetsAnimationCallback(view, callback)

        onDispose {
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }
    }

    return with(density) { topPx.toDp() }
}

private object HomeScreenDefaults {
    val HorizontalPadding = 20.dp
    val SearchActiveTopSpacing = 8.dp
    val CategoryGridSpacing = 12.dp
    const val CategoryGridColumns = 3
    const val MaxCategoryCount = 6
    const val ExpandedUpperWeight = 1f
    const val CollapsedUpperWeight = 0.001f
    const val LayoutAnimationDurationMillis = 320
    const val ContentFadeDurationMillis = 220
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
        HomeScreen(
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
            onSearchDeactivate = {},
        )
    }
}

@Preview(
    name = "Home Error",
    showBackground = true,
)
@Composable
private fun HomeErrorScreenPreview() {
    ModeraTheme {
        HomeErrorScreen()
    }
}
