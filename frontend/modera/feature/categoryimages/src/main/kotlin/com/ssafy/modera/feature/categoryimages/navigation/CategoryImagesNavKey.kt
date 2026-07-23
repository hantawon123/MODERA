package com.ssafy.modera.feature.categoryimages.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class CategoryImagesNavKey(val id: Long) : NavKey

fun Navigator.navigateToCategoryImages(
    categoryId: Long,
) {
    navigate(CategoryImagesNavKey(categoryId))
}
