package com.ssafy.modera.feature.favorite.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.favorite.FavoritesRoute

fun EntryProviderScope<NavKey>.favoritesEntry(
    onItemClick: (Long) -> Unit,
) {
    entry<FavoritesNavKey> {
        FavoritesRoute(
            onItemClick = onItemClick,
        )
    }
}
