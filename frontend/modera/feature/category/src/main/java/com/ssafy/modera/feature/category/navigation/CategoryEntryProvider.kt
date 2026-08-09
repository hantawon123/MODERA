package com.ssafy.modera.feature.category.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.category.CategoryRoute

fun EntryProviderScope<NavKey>.categoryEntry(
    onItemClick: (Long) -> Unit,
) {
    entry<CategoryNavKey> { key ->
        CategoryRoute(
            selectedCategoryId = key.selectedCategoryId,
            onItemClick = onItemClick,
        )
    }
}
