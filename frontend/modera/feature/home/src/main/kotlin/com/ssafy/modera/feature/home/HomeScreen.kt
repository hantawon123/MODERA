package com.ssafy.modera.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.component.LoadingWheel
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.feature.home.component.AiAnalysisProgressBanner
import com.ssafy.modera.feature.home.component.CategoryCard
import com.ssafy.modera.feature.home.component.CategorySortPopup
import com.ssafy.modera.feature.home.component.Header
import com.ssafy.modera.feature.home.component.SortSection

@Composable
fun HomeScreen(
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val analysisState = LocalHomeAnalysisState.current

    var previousShowBanner by remember {
        mutableStateOf(analysisState.showBanner)
    }

    LaunchedEffect(analysisState.showBanner) {
        val analysisFinished =
            previousShowBanner && !analysisState.showBanner

        if (analysisFinished) {
            viewModel.refreshCategories()
        }

        previousShowBanner = analysisState.showBanner
    }

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            HomeLoadingScreen(
                modifier = modifier,
            )
        }

        is HomeUiState.Success -> {
            HomeScreen(
                categories = state.categories,
                selectedSortType = state.selectedSortType,
                onSortTypeChange = viewModel::updateSortType,
                onCategoryClick = onCategoryClick,
                showAnalysisBanner = analysisState.showBanner,
                analysisImageCount = analysisState.imageCount,
                onDismissAnalysisBanner = analysisState::dismissBanner,
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
    categories: List<Category>,
    selectedSortType: CategorySortType,
    onSortTypeChange: (CategorySortType) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    showAnalysisBanner: Boolean = false,
    analysisImageCount: Int = 0,
    onDismissAnalysisBanner: () -> Unit = {},
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortButtonBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray)
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Header(
                title = stringResource(R.string.home_header_title),
                subtitle = stringResource(R.string.home_header_subtitle),
            )

            AnimatedVisibility(
                visible = showAnalysisBanner && analysisImageCount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                AiAnalysisProgressBanner(
                    imageCount = analysisImageCount,
                    onDismiss = onDismissAnalysisBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }

            SortSection(
                selectedSortType = selectedSortType,
                onSortClick = {
                    sortMenuExpanded = true
                },
                onPositioned = { bounds ->
                    sortButtonBounds = bounds
                },
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = categories,
                    key = Category::id,
                ) { category ->
                    CategoryCard(
                        title = category.title,
                        imageUrl = category.thumbnailUrl,
                        itemCount = category.itemCount,
                        tags = category.tags,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onCategoryClick(category)
                        },
                    )
                }
            }
        }

        if (sortMenuExpanded) {
            sortButtonBounds?.let { bounds ->
                CategorySortPopup(
                    anchorBounds = bounds,
                    selectedSortType = selectedSortType,
                    onDismissRequest = {
                        sortMenuExpanded = false
                    },
                    onSortTypeClick = { sortType ->
                        onSortTypeChange(sortType)
                        sortMenuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray),
        contentAlignment = Alignment.Center,
    ) {
        LoadingWheel()
    }
}

@Composable
private fun HomeErrorScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_error_message),
            style = ModeraTheme.typography.body2Medium,
            color = ModeraTheme.colors.typo,
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
        HomeScreen(
            categories = previewData.categories,
            selectedSortType = previewData.selectedSortType,
            onSortTypeChange = {},
            onCategoryClick = {},
        )
    }
}

@Preview(
    name = "Home Loading",
    showBackground = true,
)
@Composable
private fun HomeLoadingScreenPreview() {
    ModeraTheme {
        HomeLoadingScreen()
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