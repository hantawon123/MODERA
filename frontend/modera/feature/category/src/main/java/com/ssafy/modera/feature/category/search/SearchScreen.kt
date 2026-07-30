package com.ssafy.modera.feature.category.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.component.item.ModeraSearchBar
import com.ssafy.modera.core.component.item.SearchBarMode
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.category.CategoryMaterialUiModel
import com.ssafy.modera.feature.category.R

@Composable
fun CategorySearchRoute(
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategorySearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        CategorySearchUiState.Loading -> {
            LoadingScreen(modifier = modifier)
        }

        is CategorySearchUiState.Error -> {
            CategorySearchErrorScreen(
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }

        is CategorySearchUiState.Success -> {
            CategorySearchScreen(
                uiState = state,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onBackClick = onBackClick,
                onItemClick = onItemClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CategorySearchErrorScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTopPadding = rememberRawStatusBarTopPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(top = statusBarTopPadding),
    ) {
        ModeraTopBar(onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SearchScreenDefaults.HorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.category_search_load_error),
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CategorySearchScreen(
    uiState: CategorySearchUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    val statusBarTopPadding = rememberRawStatusBarTopPadding()

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(top = statusBarTopPadding),
    ) {
        ModeraTopBar(onBackClick = onBackClick)

        Spacer(Modifier.height(2.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = SearchScreenDefaults.HorizontalPadding),
        ) {
            ModeraSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(R.string.category_search_placeholder),
                mode = SearchBarMode.General,
                focusRequester = searchFocusRequester,
            )

            when (val content = uiState.content) {
                CategorySearchUiState.Content.RecentEmpty -> {
                    RecentMaterialsEmptySection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                is CategorySearchUiState.Content.RecentMaterials -> {
                    RecentMaterialsSection(
                        materials = content.materials,
                        onItemClick = onItemClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                is CategorySearchUiState.Content.SearchResults -> {
                    CategorySearchResultSection(
                        searchResults = content.results,
                        onItemClick = onItemClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentMaterialsEmptySection(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        RecentMaterialsHeader()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.category_search_recent_empty),
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecentMaterialsSection(
    materials: List<CategoryMaterialUiModel>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            RecentMaterialsHeader()
        }

        items(
            items = materials,
            key = { it.id },
        ) { material ->
            ModeraMaterialItem(
                title = material.title,
                description = material.description,
                tags = material.tags,
                imageUrl = material.imageUrl,
                imageCountBadge = material.imageCount,
                onClick = { onItemClick(material.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentMaterialsHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.category_search_recent_materials),
        style = ModeraTheme.typography.bodyR14,
        color = ModeraTheme.colors.gray500,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CategorySearchResultSection(
    searchResults: List<CategoryMaterialUiModel>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (searchResults.isEmpty()) {
        Box(
            modifier = modifier.padding(top = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.category_search_result_empty),
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier = modifier) {
        item {
            Row(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.category_search_result_prefix),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                )
                Text(
                    text = searchResults.size.toString(),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.yellow800,
                )
            }
        }

        items(
            items = searchResults,
            key = { it.id },
        ) { material ->
            ModeraMaterialItem(
                title = material.title,
                description = material.description,
                tags = material.tags,
                imageUrl = material.imageUrl,
                imageCountBadge = material.imageCount,
                onClick = { onItemClick(material.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal object SearchScreenDefaults {
    val HorizontalPadding = 20.dp
}

@Composable
internal fun rememberRawStatusBarTopPadding(): Dp {
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Recent Empty")
@Composable
private fun CategorySearchScreenRecentEmptyPreview() {
    ModeraTheme {
        CategorySearchScreen(
            uiState = CategorySearchUiState.Success(),
            onSearchQueryChange = {},
            onBackClick = {},
            onItemClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Recent Materials")
@Composable
private fun CategorySearchScreenRecentPreview() {
    ModeraTheme {
        CategorySearchScreen(
            uiState = CategorySearchUiState.Success(
                recentMaterials = CategorySearchDummyData.recentMaterials,
            ),
            onSearchQueryChange = {},
            onBackClick = {},
            onItemClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Search Results")
@Composable
private fun CategorySearchScreenResultsPreview() {
    ModeraTheme {
        CategorySearchScreen(
            uiState = CategorySearchUiState.Success(
                searchQuery = "케이크",
                recentMaterials = CategorySearchDummyData.recentMaterials,
                searchResults = CategorySearchDummyData.allMaterials,
            ),
            onSearchQueryChange = {},
            onBackClick = {},
            onItemClick = {},
        )
    }
}
