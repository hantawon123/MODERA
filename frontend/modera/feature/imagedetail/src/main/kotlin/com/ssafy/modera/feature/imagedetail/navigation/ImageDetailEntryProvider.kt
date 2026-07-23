package com.ssafy.modera.feature.imagedetail.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageCategory
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageOcr
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
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
        fileName = "ascii_hackathon_poster.jpg",
        status = ImageAnalysisStatus.COMPLETED,
        favorite = true,
        title = "ASCII HACKATHON",
        summary = "2026년도 제1회 대학 연합 해커톤 ASCII HACKATHON 포스터입니다.",
        ocr = AnalyzedImageOcr(
            rawText = """
            ASCII HACKATHON
            2026. 1. 30. (금) - 1. 31. (토)
        """.trimIndent(),
            refinedText = null,
            language = "ko",
            confidence = 0.99,
        ),
        tags = listOf(
            "해커톤",
            "SW",
        ),
        categories = AnalyzedImageCategory(
            categoryId = 3L,
            name = "개발",
        ),
        imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
        createdAt = "2026-07-17T19:23:00",
        updatedAt = "2026-07-17T19:23:00",
    )
)
