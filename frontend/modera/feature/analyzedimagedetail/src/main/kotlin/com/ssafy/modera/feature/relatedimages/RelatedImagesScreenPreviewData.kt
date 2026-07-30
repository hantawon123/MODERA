package com.ssafy.modera.feature.relatedimages

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus

internal data class RelatedImagesScreenPreviewData(
    val name: String,
    val sourceTitle: String,
    val uiState: RelatedImagesUiState,
) {
    override fun toString(): String = name
}

internal class RelatedImagesScreenPreviewParameterProvider :
    PreviewParameterProvider<RelatedImagesScreenPreviewData> {

    override val values: Sequence<RelatedImagesScreenPreviewData> =
        sequenceOf(
            RelatedImagesScreenPreviewData(
                name = "Success",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedImagesUiState.Success(
                    relatedImages = previewRelatedImages,
                ),
            ),
            RelatedImagesScreenPreviewData(
                name = "Loading",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedImagesUiState.Loading,
            ),
            RelatedImagesScreenPreviewData(
                name = "Empty",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedImagesUiState.Empty,
            ),
            RelatedImagesScreenPreviewData(
                name = "Error",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedImagesUiState.Error(
                    message = "연관 자료를 불러오지 못했습니다.",
                ),
            ),
        )
}

private val previewRelatedImages = List(3) { index ->
    AnalyzedImage(
        id = (index + 1).toLong(),
        title = "성심당 케이크 리스트",
        summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루와 귤 시루 등을 소개합니다.",
        thumbnailUrl = "https://picsum.photos/seed/related-image-${index + 1}/300/300",
        hashtags = listOf(
            "기차",
            "예약",
            "KTX",
        ),
        status = ImageAnalysisStatus.COMPLETED,
        favorite = false,
    )
}