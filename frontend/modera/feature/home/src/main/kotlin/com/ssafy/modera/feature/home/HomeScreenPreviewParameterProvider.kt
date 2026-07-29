package com.ssafy.modera.feature.home

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType

internal data class HomeScreenPreviewData(
    val categories: List<Category>,
    val selectedSortType: CategorySortType,
)

internal class HomeScreenPreviewParameterProvider :
    PreviewParameterProvider<HomeScreenPreviewData> {

    override val values: Sequence<HomeScreenPreviewData> = sequenceOf(
        HomeScreenPreviewData(
            categories = previewCategories,
            selectedSortType = CategorySortType.NAME_ASC,
        ),
        HomeScreenPreviewData(
            categories = emptyList(),
            selectedSortType = CategorySortType.IMAGE_COUNT_DESC,
        ),
    )
}

private val previewCategories = listOf(
    Category(
        id = 1,
        title = "음식",
        thumbnailUrl = null,
        itemCount = 42,
        tags = emptyList(),
    ),
    Category(
        id = 2,
        title = "쇼핑",
        thumbnailUrl = null,
        itemCount = 12,
        tags = emptyList(),
    ),
    Category(
        id = 3,
        title = "여행",
        thumbnailUrl = null,
        itemCount = 84,
        tags = emptyList(),
    ),
    Category(
        id = 4,
        title = "할인",
        thumbnailUrl = null,
        itemCount = 19,
        tags = emptyList(),
    ),
    Category(
        id = 5,
        title = "금융",
        thumbnailUrl = null,
        itemCount = 31,
        tags = emptyList(),
    ),
    Category(
        id = 6,
        title = "취업",
        thumbnailUrl = null,
        itemCount = 7,
        tags = emptyList(),
    ),
)
