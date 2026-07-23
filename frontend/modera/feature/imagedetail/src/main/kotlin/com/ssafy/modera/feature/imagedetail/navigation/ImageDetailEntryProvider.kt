package com.ssafy.modera.feature.imagedetail.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.AnalyzedImageDetail
import com.ssafy.modera.core.model.KeyInfoItem
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.imagedetail.ImageDetailScreen

fun EntryProviderScope<NavKey>.imageDetailEntry(
    navigator: Navigator,
) {
    entry<ImageDetailNavKey> { key ->
        val image = previewImageDetails[key.imageId] ?: previewImageDetails.values.first()

        ImageDetailScreen(
            image = image,
            onBackClick = navigator::goBack,
            onCategoryClick = { /* TODO: 카테고리 검색 화면으로 이동 */ },
            onReanalyzeClick = { /* TODO: 이미지 재분석 API 연동 */ },
            onRelatedMaterialsClick = { /* TODO: 연관 자료 리스트 화면으로 이동 */ },
            onCopyOcrTextClick = { /* TODO: 클립보드 복사 */ },
            onViewImageInfoClick = { /* TODO: 이미지 정보 다이얼로그 */ },
            onDeleteClick = { /* TODO: 이미지 삭제 API 연동 */ },
            onFavoriteClick = { /* TODO: 즐겨찾기 토글 */ },
        )
    }
}

// TODO: 추후 API 연동 시 삭제
private val previewImageDetails = mapOf(
    1L to AnalyzedImageDetail(
        id = 1L,
        title = "ASCII HACKATHON",
        imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
        categoryId = 3L,
        categoryName = "개발",
        uploadedAt = "2026년 7월 17일 금요일 오후 7:23",
        hashtags = listOf("해커톤", "SW"),
        isFavorite = true,
        summary = "2026년도 제1회 대학 연합 해커톤 ASCII HACKATHON 포스터입니다. " +
            "전국 주요 대학의 우수한 인재들이 모여 혁신적인 SW 기술을 개발하는 행사로, " +
            "무박 2일간 진행됩니다.",
        keyInfo = listOf(
            KeyInfoItem("상품명", "C++ 프로그래밍 입문"),
            KeyInfoItem("판매처", "교보문고"),
            KeyInfoItem("가격", "32,000원"),
        ),
        ocrText = "2026년도 제1회 대학 연합 해커톤\n" +
            "ASCII HACKATHON\n" +
            "2026. 1. 30. (금) - 1. 31. (토)\n" +
            "무박 2일\n" +
            "전국 주요 대학의 우수한 인재들이 모여\n" +
            "혁신적인 SW 기술을 개발하는 해커톤",
    ),
    2L to AnalyzedImageDetail(
        id = 2L,
        title = "삼성전자 주가 전망 및 투자 분석",
        imageUrl = "https://picsum.photos/seed/stock_detail/800/1200",
        categoryId = 7L,
        categoryName = "금융",
        uploadedAt = "2026년 7월 16일 목요일 오후 3:10",
        hashtags = listOf("주식", "삼성전자"),
        isFavorite = false,
        summary = "삼성전자 주가 전망과 투자 분석 내용이 담긴 이미지입니다.",
        keyInfo = listOf(
            KeyInfoItem("종목명", "삼성전자"),
            KeyInfoItem("현재가", "72,500원"),
            KeyInfoItem("전망", "중립"),
        ),
        ocrText = "삼성전자 주가 전망\n현재가 72,500원\n목표가 80,000원",
    ),
)
