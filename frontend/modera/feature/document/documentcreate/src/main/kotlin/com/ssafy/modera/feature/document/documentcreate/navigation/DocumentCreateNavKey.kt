package com.ssafy.modera.feature.document.documentcreate.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCreateNavKey(
    val image: DocumentCreateImageArg,
) : NavKey

@Serializable
data class DocumentRecreateNavKey(
    val documentId: Long,
    val images: List<DocumentCreateImageArg>,
) : NavKey

fun Navigator.navigateToDocumentCreate(
    analyzedImage: AnalyzedImage,
) {
    navigate(
        DocumentCreateNavKey(
            image = analyzedImage.asNavArg(),
        ),
    )
}

fun Navigator.navigateToDocumentRecreate(
    documentId: Long,
    analyzedImages: List<AnalyzedImage>,
) {
    require(analyzedImages.isNotEmpty()) {
        "문서를 재구성하려면 이미지가 한 장 이상 필요합니다."
    }

    navigate(
        DocumentRecreateNavKey(
            documentId = documentId,
            images = analyzedImages
                .distinctBy(AnalyzedImage::id)
                .map(AnalyzedImage::asNavArg),
        ),
    )
}

private fun AnalyzedImage.asNavArg(): DocumentCreateImageArg =
    DocumentCreateImageArg(
        id = id,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl,
        hashtags = hashtags,
        favorite = favorite,
    )

internal fun DocumentCreateImageArg.asAnalyzedImage(): AnalyzedImage =
    AnalyzedImage(
        id = id,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl,
        hashtags = hashtags,
        favorite = favorite,
    )