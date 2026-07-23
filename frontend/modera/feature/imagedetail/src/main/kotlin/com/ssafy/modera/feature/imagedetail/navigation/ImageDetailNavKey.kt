package com.ssafy.modera.feature.imagedetail.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class ImageDetailNavKey(
    val imageId: Long,
) : NavKey

fun Navigator.navigateToImageDetail(
    imageId: Long,
) {
    navigate(ImageDetailNavKey(imageId))
}