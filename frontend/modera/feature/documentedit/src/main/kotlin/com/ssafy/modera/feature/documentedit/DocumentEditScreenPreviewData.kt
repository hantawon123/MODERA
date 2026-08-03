package com.ssafy.modera.feature.documentedit

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

internal data class DocumentEditScreenPreviewData(
    val name: String,
    val uiState: DocumentEditUiState,
) {
    override fun toString(): String = name
}

internal class DocumentEditScreenPreviewParameterProvider :
    PreviewParameterProvider<DocumentEditScreenPreviewData> {

    override val values: Sequence<DocumentEditScreenPreviewData> =
        sequenceOf(
            DocumentEditScreenPreviewData(
                name = "Success",
                uiState = DocumentEditUiState.Success(
                    images = previewDocumentEditImages.take(5),
                    selectedImageIds = previewDocumentEditImages
                        .take(5)
                        .map(AnalyzedImage::id)
                        .toSet(),
                ),
            ),
            DocumentEditScreenPreviewData(
                name = "Editing",
                uiState = DocumentEditUiState.Success(
                    images = previewDocumentEditImages.take(5),
                    selectedImageIds = previewDocumentEditImages
                        .take(3)
                        .map(AnalyzedImage::id)
                        .toSet(),
                    isEditing = true,
                ),
            ),
            DocumentEditScreenPreviewData(
                name = "Loading",
                uiState = DocumentEditUiState.Loading,
            ),
            DocumentEditScreenPreviewData(
                name = "Applying",
                uiState = DocumentEditUiState.Applying(
                    selectedImages = previewDocumentEditImages.take(3),
                ),
            ),
            DocumentEditScreenPreviewData(
                name = "Error",
                uiState = DocumentEditUiState.Error(
                    exception = IllegalStateException(
                        "문서의 스크린샷 목록을 불러오지 못했습니다.",
                    ),
                ),
            ),
        )
}

internal val previewDocumentEditImages =
    List(10) { index ->
        AnalyzedImage(
            id = index.toLong(),
            title = "성심당 케이크 리스트",
            summary = "올해 성심당 케이크 메뉴 리스트로, " +
                    "샤인머스켓 시루, 귤 시루, 맛있겠다.",
            thumbnailUrl =
                "https://picsum.photos/seed/" +
                        "document-edit-$index/300/300",
            hashtags = listOf(
                "기차",
                "예약",
                "KTX",
            ),
            favorite = false,
        )
    }