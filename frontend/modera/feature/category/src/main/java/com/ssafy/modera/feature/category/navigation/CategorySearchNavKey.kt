package com.ssafy.modera.feature.category.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object CategorySearchNavKey : NavKey

fun Navigator.navigateToCategorySearch() {
    navigate(CategorySearchNavKey)
}
