package com.ssafy.modera.feature.category.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.category.CategoryRoute

fun EntryProviderScope<NavKey>.categoryEntry(
    onSearchIconClick: () -> Unit,
    onItemClick: (Int) -> Unit,
) {
    entry<CategoryNavKey> {
        CategoryRoute(
            onSearchIconClick = onSearchIconClick,
            onItemClick = onItemClick,
        )
    }
}
