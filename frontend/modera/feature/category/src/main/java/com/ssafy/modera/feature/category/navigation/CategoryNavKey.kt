package com.ssafy.modera.feature.category.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class CategoryNavKey(
    val selectedCategoryId: Long? = null,
) : NavKey

fun Navigator.navigateToCategoryTab(
    selectedCategoryId: Long? = null,
) {
    val tabKey = CategoryNavKey()
    navigateToTopLevelTab(
        topLevelKey = tabKey,
        rootKey = CategoryNavKey(selectedCategoryId = selectedCategoryId),
    )
}

fun Navigator.navigateToCategoryTab(
    category: Category,
) {
    navigateToCategoryTab(selectedCategoryId = category.id)
}
