package com.ssafy.modera.navigation

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.R
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import kotlinx.serialization.Serializable

/**
 * Type for the top level navigation items in the application. Contains UI information about the
 * current route that is used in the top app bar and common navigation UI.
 *
 * @param selectedIcon The icon to be displayed in the navigation UI when this destination is
 * selected.
 * @param unselectedIcon The icon to be displayed in the navigation UI when this destination is
 * not selected.
 * @param iconTextId Text that to be displayed in the navigation UI.
 * @param titleTextId Text that is displayed on the top app bar.
 */
data class TopLevelNavItem(
    val selectedIcon: Int,
    val unselectedIcon: Int,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

val HOME = TopLevelNavItem(
    selectedIcon =  R.drawable.ic_home_selected,
    unselectedIcon = R.drawable.ic_home_unselected,
    iconTextId =  R.string.nav_bar_home,
    titleTextId = R.string.nav_bar_home,
)

val REGISTER = TopLevelNavItem(
    selectedIcon =  R.drawable.ic_register_selected,
    unselectedIcon = R.drawable.ic_register_unselected,
    iconTextId = R.string.nav_bar_register,
    titleTextId = R.string.nav_bar_register,
)

val SEARCH = TopLevelNavItem(
    selectedIcon =  R.drawable.ic_search_selected,
    unselectedIcon = R.drawable.ic_search_unselected,
    iconTextId = R.string.nav_bar_search,
    titleTextId = R.string.nav_bar_search,
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    HomeNavKey to HOME,
    RegisterNavKey to REGISTER,
    SearchNavKey to SEARCH,
)

// TODO : 추후 각 화면으로 이동
@Serializable
object RegisterNavKey : NavKey

@Serializable
object SearchNavKey : NavKey