package com.ssafy.modera.feature.documentcreate.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCreateNavKey(
    val imageId: Long,
    val title: String,
    val summary: String,
    val thumbnailUrl: String,
    val hashtags: List<String>,
    val favorite: Boolean,
) : NavKey

fun Navigator.navigateToDocumentCreate(
    analyzedImage: AnalyzedImage,
) {
    navigate(
        DocumentCreateNavKey(
            imageId = analyzedImage.id,
            title = analyzedImage.title,
            summary = analyzedImage.summary,
            thumbnailUrl = analyzedImage.thumbnailUrl,
            hashtags = analyzedImage.hashtags,
            favorite = analyzedImage.favorite,
        ),
    )
}

internal fun DocumentCreateNavKey.asInitialImage(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl,
        hashtags = hashtags,
        favorite = favorite,
    )