package com.ssafy.modera.feature.category.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.category.CategoryTabController
import com.ssafy.modera.feature.category.di.CategoryTabControllerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

@Serializable
data class CategoryNavKey(
    val selectedCategoryId: Long? = null,
) : NavKey

fun Navigator.navigateToCategoryTab(
    categoryTabController: CategoryTabController,
) {
    if (state.currentTopLevelKey == CategoryNavKey()) {
        navigate(CategoryNavKey())
        categoryTabController.showAll()
        return
    }

    navigateToTopLevelTab(
        topLevelKey = CategoryNavKey(),
        rootKey = CategoryNavKey(),
    )
    categoryTabController.showAll()
}

fun Navigator.navigateToCategoryTab(
    selectedCategoryId: Long? = null,
) {
    navigateToTopLevelTab(
        topLevelKey = CategoryNavKey(),
        rootKey = CategoryNavKey(selectedCategoryId = selectedCategoryId),
    )
}

fun Navigator.navigateToCategoryTab(
    category: Category,
) {
    navigateToCategoryTab(selectedCategoryId = category.id)
}

@Composable
fun rememberCategoryTabController(): CategoryTabController {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CategoryTabControllerEntryPoint::class.java,
        ).categoryTabController()
    }
}
