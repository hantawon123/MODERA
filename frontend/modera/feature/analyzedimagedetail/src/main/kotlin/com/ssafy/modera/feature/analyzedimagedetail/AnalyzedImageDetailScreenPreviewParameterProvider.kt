package com.ssafy.modera.feature.analyzedimagedetail

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail

internal data class AnalyzedImageDetailScreenPreviewData(
    val name: String,
    val uiState: AnalyzedImageDetailUiState,
) {
    override fun toString(): String = name
}

internal class AnalyzedImageDetailScreenPreviewParameterProvider :
    PreviewParameterProvider<AnalyzedImageDetailScreenPreviewData> {

    override val values: Sequence<AnalyzedImageDetailScreenPreviewData> =
        sequenceOf(
            AnalyzedImageDetailScreenPreviewData(
                name = "Success",
                uiState = AnalyzedImageDetailUiState.Success(
                    image = previewAnalyzedImageDetail,
                ),
            ),
            AnalyzedImageDetailScreenPreviewData(
                name = "Loading",
                uiState = AnalyzedImageDetailUiState.Loading,
            ),
            AnalyzedImageDetailScreenPreviewData(
                name = "Error",
                uiState = AnalyzedImageDetailUiState.Error(
                    exception = IllegalStateException(
                        "이미지 정보를 불러오지 못했습니다.",
                    ),
                ),
            ),
        )
}

private val previewAnalyzedImageDetail = AnalyzedImageDetail(
    id = 1L,
    imageUrl = "https://picsum.photos/seed/samsung-stock/600/800",
    thumbnailUrl = "https://picsum.photos/seed/samsung-stock/300/400",
    title = "삼성전자 주가 정보",
    favorite = false,
    summary = """
        삼성전자 주식의 현재 가격과 등락 정보를 보여주는 화면입니다.
        현재 주가는 255,000원이며 전일 대비 24,500원 하락했습니다.
        등락률은 -8.77%로, 단기적으로 큰 폭의 변동이 발생했습니다.
        
        투자 판단 전 최근 실적과 시장 흐름을 함께 확인할 필요가 있습니다.
    """.trimIndent(),
    category = "금융",
    tags = listOf(
        "삼성전자",
        "주식",
        "KOSPI",
    ),
    extractedTexts = listOf(
        "삼성",
        "삼성전자",
        "005930",
        "KOSPI",
        "관심종목",
        "255,000원",
        "-24,500",
        "-8.77%",
    ),
    keyInformation = listOf(
        "종목 코드: 005930",
        "시장: KOSPI",
        "현재가: 255,000원",
        "전일 대비: -24,500원",
        "등락률: -8.77%",
    ),
    isDocumented = false,
    isCalendared = false,
    updatedAt = 1785376620000L,
)