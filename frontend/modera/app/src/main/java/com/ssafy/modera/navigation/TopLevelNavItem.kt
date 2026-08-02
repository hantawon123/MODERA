package com.ssafy.modera.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.R
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.feature.category.navigation.CategoryNavKey
import com.ssafy.modera.feature.document.navigation.DocumentNavKey
import com.ssafy.modera.feature.favorite.navigation.FavoritesNavKey
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import kotlinx.serialization.Serializable

/**
 * Type for the top level navigation items in the application. Contains UI information about the
 * current route that is used in the top app bar and common navigation UI.
 *
 * @param icon The outline icon displayed in the bottom navigation.
 * @param iconTextId Text displayed under the icon. Null for the center action button.
 * @param titleTextId Text displayed on the top app bar.
 * @param isCenterAction Whether this item is the primary center action (no label, yellow button).
 */
data class TopLevelNavItem(
    @DrawableRes val icon: Int,
    @StringRes val iconTextId: Int?,
    @StringRes val titleTextId: Int,
    val isCenterAction: Boolean = false,
)

val HOME = TopLevelNavItem(
    icon = ModeraIcons.Home,
    iconTextId = R.string.nav_bar_home,
    titleTextId = R.string.nav_bar_home,
)

val CATEGORY = TopLevelNavItem(
    icon = ModeraIcons.Category,
    iconTextId = R.string.nav_bar_category,
    titleTextId = R.string.nav_bar_category,
)

val REGISTER = TopLevelNavItem(
    icon = ModeraIcons.Add,
    iconTextId = null,
    titleTextId = R.string.nav_bar_register,
    isCenterAction = true,
)

val FAVORITES = TopLevelNavItem(
    icon = ModeraIcons.Heart,
    iconTextId = R.string.nav_bar_favorites,
    titleTextId = R.string.nav_bar_favorites,
)

val DOCUMENTS = TopLevelNavItem(
    icon = ModeraIcons.BottomNavFile,
    iconTextId = R.string.nav_bar_documents,
    titleTextId = R.string.nav_bar_documents,
)

/** Top-level destinations that own a navigation stack (excludes the center action). */
val TOP_LEVEL_NAV_ITEMS = mapOf(
    HomeNavKey to HOME,
    CategoryNavKey() to CATEGORY,
    FavoritesNavKey to FAVORITES,
    DocumentNavKey to DOCUMENTS,
)

/** Ordered bottom navigation slots, including the center register action. */
val BOTTOM_NAV_ITEMS = listOf(
    HomeNavKey to HOME,
    CategoryNavKey() to CATEGORY,
    RegisterNavKey to REGISTER,
    FavoritesNavKey to FAVORITES,
    DocumentNavKey to DOCUMENTS,
)

/* TODO: 추후 코드 이동 */
@Serializable
object RegisterNavKey : NavKey
