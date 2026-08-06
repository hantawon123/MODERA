package com.ssafy.modera.feature.analyzedimage.api.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageDetailNavKey(
    val imageId: Long,
) : NavKey

fun Navigator.navigateToImageDetail(
    imageId: Long,
) {
    navigate(AnalyzedImageDetailNavKey(imageId))
}