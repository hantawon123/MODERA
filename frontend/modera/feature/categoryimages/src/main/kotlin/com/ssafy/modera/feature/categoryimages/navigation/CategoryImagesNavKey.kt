package com.ssafy.modera.feature.categoryimages.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class CategoryImagesNavKey(val category: Category) : NavKey

fun Navigator.navigateToCategoryImages(
    category: Category,
) {
    navigate(CategoryImagesNavKey(category))
}
