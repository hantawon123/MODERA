package com.ssafy.modera.feature.categoryimages.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CategoryImagesNavKey(
    val categoryId: Long,
    val categoryName: String,
) : NavKey
