package com.ssafy.modera.feature.documentdetail

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ssafy.modera.core.model.DocumentDetail

internal data class DocumentDetailScreenPreviewData(
    val name: String,
    val uiState: DocumentDetailUiState,
) {
    override fun toString(): String = name
}

internal class DocumentDetailScreenPreviewParameterProvider :
    PreviewParameterProvider<DocumentDetailScreenPreviewData> {

    override val values: Sequence<DocumentDetailScreenPreviewData> =
        sequenceOf(
            DocumentDetailScreenPreviewData(
                name = "Success",
                uiState = DocumentDetailUiState.Success(
                    document = previewDocumentDetail,
                ),
            ),
            DocumentDetailScreenPreviewData(
                name = "Reanalyzing",
                uiState = DocumentDetailUiState.Reanalyzing,
            ),
            DocumentDetailScreenPreviewData(
                name = "Loading",
                uiState = DocumentDetailUiState.Loading,
            ),
            DocumentDetailScreenPreviewData(
                name = "Error",
                uiState = DocumentDetailUiState.Error(
                    exception = IllegalStateException(
                        "문서 상세 조회에 실패했습니다.",
                    ),
                ),
            ),
        )
}

private val previewDocumentDetail = DocumentDetail(
    id = 1L,
    name = "AI 도시·지역혁신 공모전 및 정보처리기사 자격 정보",
    summary = "AI 기술을 접목한 도시 및 지역혁신 아이디어 공모전 안내와 " +
            "정보처리기사 국가기술자격의 기본 정보를 정리한 문서입니다.",
    content = """
        ## AI 도시·지역혁신 아이디어 공모전
        
        국토교통부가 주관하는 2026 대한민국 도시·지역혁신 산업박람회에서
        AI 기술을 기반으로 한 도시 및 지역혁신 아이디어를 공모합니다.
        
        - 공모분야: 건설혁신형, 주거개선형, 혁신행정형, 생활서비스형
        - 참가대상: 관련 민간·공공분야 종사자 및 전문가, 대학(원)생 등
        - 접수기간: 2026년 8월 28일 금요일 18시까지
        - 제출방법: 이메일 및 우편 또는 방문 제출
        - 문의처: 도시·지역혁신 산업박람회 운영사무국
        
        > 출처: #52
        
        ## 정보처리기사 국가기술자격 정보
        
        큐넷을 통해 제공되는 정보처리기사 국가기술자격의 기본 현황입니다.
        
        - 자격증명: 정보처리기사
        - 관련부처: 과학기술정보통신부
        - 시행기관: 한국산업인력공단
        
        > 출처: #61
        
        ---
        
        ## 출처
        
        | 이미지 | 제목 | 카테고리 | 저장 시각 |
        | --- | --- | --- | --- |
        | #52 | AI 도시·지역혁신 아이디어 공모전 | 일정 | 2026-07-31 |
        | #61 | 정보처리기사 국가자격 종목 상세정보 | 학습 | 2026-08-01 |
    """.trimIndent(),
    imageCount = 8,
    deletedImageCount = 0,
    imageIds = listOf(
        52L,
        61L,
        62L,
        63L,
        64L,
        65L,
        66L,
        67L,
    ),
    regenerating = false,
    updatedAt = 1_785_087_600_000L,
)