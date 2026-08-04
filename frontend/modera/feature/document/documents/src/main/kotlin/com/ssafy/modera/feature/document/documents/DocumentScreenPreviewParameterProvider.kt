package com.ssafy.modera.feature.document

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import com.ssafy.modera.feature.document.documents.DocumentUiState

internal data class DocumentScreenPreviewData(
    val name: String,
    val uiState: DocumentUiState,
) {
    override fun toString(): String = name
}

internal class DocumentScreenPreviewParameterProvider :
    PreviewParameterProvider<DocumentScreenPreviewData> {

    override val values: Sequence<DocumentScreenPreviewData> =
        sequenceOf(
            DocumentScreenPreviewData(
                name = "Success",
                uiState = DocumentUiState.Success(
                    documents = previewDocuments,
                    sortType = DocumentSortType.LATEST,
                ),
            ),
            DocumentScreenPreviewData(
                name = "Empty",
                uiState = DocumentUiState.Empty,
            ),
            DocumentScreenPreviewData(
                name = "Loading",
                uiState = DocumentUiState.Loading,
            ),
            DocumentScreenPreviewData(
                name = "Error",
                uiState = DocumentUiState.Error(
                    exception = IllegalStateException(
                        "문서를 불러오지 못했습니다.",
                    ),
                ),
            ),
        )
}

private val previewDocuments = listOf(
    Document(
        id = 1L,
        title = "오사카 3박 4일 여행 계획",
        content = """
            항공권, 숙소, 맛집 정보를 분석해 날짜별 일정과 추천 코스로 정리했어요.
            주요 관광지의 운영 시간과 이동 경로도 함께 확인할 수 있습니다.
        """.trimIndent(),
        sourceImageCount = 8,
        updatedAt = 1_785_114_000_000L,
    ),
    Document(
        id = 2L,
        title = "삼성전자 주가 분석 요약",
        content = """
            최근 주가 흐름과 주요 지표를 분석하고 전망과 투자 포인트를 정리했어요.
            실적 발표와 시장 흐름을 함께 확인할 수 있습니다.
        """.trimIndent(),
        sourceImageCount = 6,
        updatedAt = 1_785_027_600_000L,
    ),
    Document(
        id = 3L,
        title = "노이즈 캔슬링 이어폰 비교",
        content = """
            주요 브랜드 이어폰의 스펙과 가격을 비교하고 추천 제품을 정리했어요.
            음질, 배터리, 착용감 등의 특징을 한눈에 볼 수 있습니다.
        """.trimIndent(),
        sourceImageCount = 5,
        updatedAt = 1_784_941_200_000L,
    ),
    Document(
        id = 4L,
        title = "카카오 Android 개발자 채용 정리",
        content = """
            공고 핵심 내용과 지원 자격, 우대사항을 한눈에 보기 쉽게 정리했어요.
            채용 일정과 필요 기술도 함께 확인할 수 있습니다.
        """.trimIndent(),
        sourceImageCount = 4,
        updatedAt = 1_784_854_800_000L,
    ),
    Document(
        id = 5L,
        title = "서울 전시회 일정 모음",
        content = """
            관심 있는 전시회의 일정과 장소, 예매 정보를 정리했어요.
            종료일이 가까운 전시회를 우선적으로 확인할 수 있습니다.
        """.trimIndent(),
        sourceImageCount = 7,
        updatedAt = 1_784_768_400_000L,
    ),
)