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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.Category
import com.ssafy.modera.feature.home.component.AiAnalysisProgressBanner
import com.ssafy.modera.feature.home.component.CategoryCard
import com.ssafy.modera.feature.home.component.CategorySortPopup
import com.ssafy.modera.feature.home.component.Header
import com.ssafy.modera.feature.home.component.SortSection

@Composable
fun HomeScreen(
    categories: List<Category>,
    selectedSortType: CategorySortType,
    onSortTypeChange: (CategorySortType) -> Unit,
    onCategoryClick: (Long) -> Unit,
    showAnalysisBanner: Boolean = false,
    analysisImageCount: Int = 0,
    onDismissAnalysisBanner: () -> Unit = {},
    modifier: Modifier = Modifier,
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
                subtitle = stringResource(R.string.home_header_subtitle)
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
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
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
                            onCategoryClick(category.id)
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

enum class CategorySortType(
    val label: String,
) {
    NAME("이름순"), LATEST("최신 업로드순"), IMAGE_COUNT("사진 많은 순"),
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