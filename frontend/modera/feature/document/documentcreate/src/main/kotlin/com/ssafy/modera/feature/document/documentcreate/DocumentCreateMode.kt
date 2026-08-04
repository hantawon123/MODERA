package com.ssafy.modera.feature.document.documentcreate

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface DocumentCreateMode {

    val initialImages: List<AnalyzedImage>

    data class Create(
        val initialImage: AnalyzedImage,
    ) : DocumentCreateMode {

        override val initialImages: List<AnalyzedImage> =
            listOf(initialImage)
    }

    data class Recreate(
        val documentId: Long,
        override val initialImages: List<AnalyzedImage>,
    ) : DocumentCreateMode
}