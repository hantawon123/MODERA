package com.ssafy.modera.feature.imageviewer.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class ImageViewerNavKey(val imageUrl: String) : NavKey

fun Navigator.navigateToImageViewer(
    imageUrl: String,
) {
    navigate(ImageViewerNavKey(imageUrl))
}