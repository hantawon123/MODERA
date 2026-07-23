package com.ssafy.modera.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.home.HomeScreen

fun EntryProviderScope<NavKey>.homeEntry(
    navigator: Navigator,
    onCategoryClick: (Long) -> Unit,
) {
    entry<HomeNavKey> {
        HomeScreen(
            onCategoryClick = onCategoryClick,
        )
    }
}
