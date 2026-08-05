package com.ssafy.modera.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.home.HomeTabController
import com.ssafy.modera.feature.home.di.HomeTabControllerEntryPoint
import dagger.hilt.android.EntryPointAccessors

fun Navigator.navigateToHomeTab(
    homeTabController: HomeTabController,
) {
    navigate(HomeNavKey)
    homeTabController.resetToDefault()
}

@Composable
fun rememberHomeTabController(): HomeTabController {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeTabControllerEntryPoint::class.java,
        ).homeTabController()
    }
}
