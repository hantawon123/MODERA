package com.ssafy.modera.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ssafy.modera.core.navigation.NavigationState
import com.ssafy.modera.core.navigation.rememberNavigationState
import com.ssafy.modera.navigation.HomeNavKey
import com.ssafy.modera.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberModeraAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): ModeraAppState {
    val navigationState = rememberNavigationState(HomeNavKey, TOP_LEVEL_NAV_ITEMS.keys)

    return remember(
        navigationState,
        coroutineScope,
    ) {
        ModeraAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class ModeraAppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
) {
    // TODO : 추후 필요한 기능 관련 state 추가
}

