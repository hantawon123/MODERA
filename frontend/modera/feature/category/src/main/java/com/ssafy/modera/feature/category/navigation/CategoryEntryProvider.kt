package com.ssafy.modera.feature.category.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.category.CategoryRoute
import com.ssafy.modera.feature.category.search.CategorySearchRoute

fun EntryProviderScope<NavKey>.categoryEntry(
    onBackClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onItemClick: (Long) -> Unit,
) {
    entry<CategoryNavKey> {
        CategoryRoute(
            onSearchIconClick = onSearchIconClick,
            onItemClick = onItemClick,
        )
    }

    entry<CategorySearchNavKey> {
        CategorySearchRoute(
            onBackClick = onBackClick,
            onItemClick = onItemClick,
        )
    }
}
