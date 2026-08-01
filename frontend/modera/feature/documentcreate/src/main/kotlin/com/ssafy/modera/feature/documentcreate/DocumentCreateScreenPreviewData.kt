package com.ssafy.modera.feature.documentcreate

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus

internal data class DocumentCreateScreenPreviewData(
    val name: String,
    val uiState: DocumentCreateUiState,
    val selectedImages: List<AnalyzedImage>,
) {
    override fun toString(): String = name
}

internal class DocumentCreateScreenPreviewParameterProvider :
    PreviewParameterProvider<DocumentCreateScreenPreviewData> {

    override val values: Sequence<DocumentCreateScreenPreviewData> =
        sequenceOf(
            DocumentCreateScreenPreviewData(
                name = "Success",
                uiState = DocumentCreateUiState.Success(
                    recommendedImages =
                        previewDocumentCreateImages.drop(5),
                ),
                selectedImages =
                    previewDocumentCreateImages.take(5),
            ),
            DocumentCreateScreenPreviewData(
                name = "Loading",
                uiState = DocumentCreateUiState.Loading,
                selectedImages =
                    previewDocumentCreateImages.take(3),
            ),
            DocumentCreateScreenPreviewData(
                name = "Creating",
                uiState = DocumentCreateUiState.Creating,
                selectedImages =
                    previewDocumentCreateImages.take(5),
            ),
            DocumentCreateScreenPreviewData(
                name = "Error",
                uiState = DocumentCreateUiState.Error(
                    exception = IllegalStateException(
                        "추천 자료를 불러오지 못했습니다.",
                    ),
                ),
                selectedImages =
                    previewDocumentCreateImages.take(3),
            ),
        )
}

internal val previewDocumentCreateImages =
    List(20) { index ->
        AnalyzedImage(
            id = index.toLong(),
            title = "성심당 케이크 리스트",
            summary = "올해 성심당 케이크 메뉴 리스트로, " +
                    "샤인머스켓 시루, 귤 시루, 맛있겠다.",
            thumbnailUrl =
                "https://picsum.photos/seed/" +
                        "document-create-$index/300/300",
            hashtags = listOf(
                "기차",
                "예약",
                "KTX",
            ),
            favorite = false,
        )
    }