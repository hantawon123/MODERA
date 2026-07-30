package com.ssafy.modera.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.home.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    navigator: Navigator,
    onCategoryClick: (Category) -> Unit,
) {
    entry<HomeNavKey> {
        HomeRoute(
            onCategoryClick = onCategoryClick,
            onCalendarClick = { /* TODO: 추후 네비게이션 연결 */ },
            onSettingsClick = { /* TODO: 추후 네비게이션 연결 */ },
            onSearchResultClick = { /* TODO: 추후 네비게이션 연결 */ },
        )
    }
}
