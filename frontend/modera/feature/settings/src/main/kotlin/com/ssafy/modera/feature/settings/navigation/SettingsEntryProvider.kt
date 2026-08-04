package com.ssafy.modera.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.settings.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    entry<SettingsNavKey> {
        SettingsRoute(
            onBackClick = onBackClick,
            onLogoutClick = onLogoutClick,
        )
    }
}
