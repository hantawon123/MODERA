package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.feature.home.HomeScreenDefaults

@Composable
internal fun HomeCategoryGrid(
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
                            imageUrl = category.thumbnailUrl,
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
