package com.ssafy.modera.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.feature.home.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    onCategoryClick: (Category) -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchResultClick: (Long) -> Unit,
) {
    entry<HomeNavKey> {
        HomeRoute(
            onCategoryClick = onCategoryClick,
            onCalendarClick = onCalendarClick,
            onSettingsClick = onSettingsClick,
            onSearchResultClick = onSearchResultClick,
        )
    }
}
