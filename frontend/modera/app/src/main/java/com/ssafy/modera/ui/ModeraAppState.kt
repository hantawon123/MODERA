package com.ssafy.modera.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ssafy.modera.core.navigation.NavigationState
import com.ssafy.modera.core.navigation.rememberNavigationState
import com.ssafy.modera.feature.category.CategoryTabController
import com.ssafy.modera.feature.category.navigation.rememberCategoryTabController
import com.ssafy.modera.feature.home.HomeTabController
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import com.ssafy.modera.feature.home.navigation.rememberHomeTabController
import com.ssafy.modera.feature.onboading.api.navigation.OnboardingNavKey
import com.ssafy.modera.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberModeraAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): ModeraAppState {
    val navigationState = rememberNavigationState(
        HomeNavKey, TOP_LEVEL_NAV_ITEMS.keys,
        OnboardingNavKey
    )
    val homeTabController = rememberHomeTabController()
    val categoryTabController = rememberCategoryTabController()

    return remember(
        navigationState,
        homeTabController,
        categoryTabController,
        coroutineScope,
    ) {
        ModeraAppState(
            navigationState = navigationState,
            homeTabController = homeTabController,
            categoryTabController = categoryTabController,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class ModeraAppState(
    val navigationState: NavigationState,
    val homeTabController: HomeTabController,
    val categoryTabController: CategoryTabController,
    coroutineScope: CoroutineScope,
) {
    // TODO : 추후 필요한 기능 관련 state 추가
}
