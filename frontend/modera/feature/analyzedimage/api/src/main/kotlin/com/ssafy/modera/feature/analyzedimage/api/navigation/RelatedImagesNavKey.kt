package com.ssafy.modera.feature.analyzedimage.api.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class RelatedImagesNavKey(
    val imageId: Long,
    val sourceTitle: String,
) : NavKey

fun Navigator.navigateToRelatedImages(
    imageId: Long,
    sourceTitle: String,
) {
    navigate(
        RelatedImagesNavKey(
            imageId = imageId,
            sourceTitle = sourceTitle,
        ),
    )
}