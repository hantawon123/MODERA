package com.ssafy.modera.feature.analyzedimage.api.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class RelatedDocumentsNavKey(
    val imageId: Long,
    val sourceTitle: String,
) : NavKey

fun Navigator.navigateToRelatedDocuments(
    imageId: Long,
    sourceTitle: String,
) {
    navigate(
        RelatedDocumentsNavKey(
            imageId = imageId,
            sourceTitle = sourceTitle,
        ),
    )
}