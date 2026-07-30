package com.ssafy.modera.feature.category.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.component.item.ModeraSearchBar
import com.ssafy.modera.core.component.item.SearchBarMode
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.category.R

@Composable
fun CategorySearchRoute(
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit,
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Vertical,
                )
            ),
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
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white).windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Vertical,
                ),
            ),
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
                    RecentAnalyzedImagesEmptySection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                is CategorySearchUiState.Content.RecentAnalyzedImages -> {
                    RecentAnalyzedImagesSection(
                        analyzedImages = content.analyzedImages,
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
private fun RecentAnalyzedImagesEmptySection(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        RecentAnalyzedImagesHeader()

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
private fun RecentAnalyzedImagesSection(
    analyzedImages: List<AnalyzedImage>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            RecentAnalyzedImagesHeader()
        }

        items(
            items = analyzedImages,
            key = { it.id },
        ) { analyzedImage ->
            ModeraMaterialItem(
                title = analyzedImage.title,
                description = analyzedImage.summary,
                tags = analyzedImage.hashtags,
                imageUrl = analyzedImage.thumbnailUrl,
                onClick = { onItemClick(analyzedImage.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentAnalyzedImagesHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.category_search_recent_analyzed_images),
        style = ModeraTheme.typography.bodyR14,
        color = ModeraTheme.colors.gray500,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CategorySearchResultSection(
    searchResults: List<AnalyzedImage>,
    onItemClick: (Long) -> Unit,
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
        ) { analyzedImage ->
            ModeraMaterialItem(
                title = analyzedImage.title,
                description = analyzedImage.summary,
                tags = analyzedImage.hashtags,
                imageUrl = analyzedImage.thumbnailUrl,
                onClick = { onItemClick(analyzedImage.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal object SearchScreenDefaults {
    val HorizontalPadding = 20.dp
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Recent AnalyzedImages")
@Composable
private fun CategorySearchScreenRecentPreview() {
    ModeraTheme {
        CategorySearchScreen(
            uiState = CategorySearchUiState.Success(
                recentAnalyzedImages = CategorySearchDummyData.recentMaterials,
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
                recentAnalyzedImages = CategorySearchDummyData.recentMaterials,
                searchResults = CategorySearchDummyData.allMaterials,
            ),
            onSearchQueryChange = {},
            onBackClick = {},
            onItemClick = {},
        )
    }
}
