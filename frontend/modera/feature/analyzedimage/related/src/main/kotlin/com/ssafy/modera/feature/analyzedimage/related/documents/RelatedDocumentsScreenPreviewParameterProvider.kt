package com.ssafy.modera.feature.analyzedimage.related.documents

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.document.Document

internal data class RelatedDocumentsScreenPreviewData(
    val name: String,
    val sourceTitle: String,
    val uiState: RelatedDocumentsUiState,
) {
    override fun toString(): String = name
}

internal class RelatedDocumentsScreenPreviewParameterProvider :
    PreviewParameterProvider<RelatedDocumentsScreenPreviewData> {

    override val values: Sequence<RelatedDocumentsScreenPreviewData> =
        sequenceOf(
            RelatedDocumentsScreenPreviewData(
                name = "Success",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedDocumentsUiState.Success(
                    relatedDocuments = previewRelatedDocuments,
                ),
            ),
            RelatedDocumentsScreenPreviewData(
                name = "Loading",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedDocumentsUiState.Loading,
            ),
            RelatedDocumentsScreenPreviewData(
                name = "Empty",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedDocumentsUiState.Empty,
            ),
            RelatedDocumentsScreenPreviewData(
                name = "Error",
                sourceTitle = "ASCII 해커톤",
                uiState = RelatedDocumentsUiState.Error(
                    exception = IllegalStateException(
                        "연관 문서를 불러오지 못했습니다.",
                    ),
                ),
            ),
        )
}

private val previewRelatedDocuments = List(3) { index ->
    Document(
        id = (index + 1).toLong(),
        title = when (index) {
            0 -> "ASCII 해커톤 참가 안내"
            1 -> "프로젝트 아이디어 정리"
            else -> "해커톤 준비 체크리스트"
        },
        content = when (index) {
            0 -> "ASCII 해커톤의 일정과 참가 방법을 정리한 문서입니다."
            1 -> "팀에서 논의한 프로젝트 아이디어와 주요 기능을 정리했습니다."
            else -> "발표 자료, 시연 영상, 제출 서류 등 준비 항목을 정리했습니다."
        },
        sourceImageCount = index + 2,
        updatedAt = 1_754_455_200_000L - index * 86_400_000L,
    )
}