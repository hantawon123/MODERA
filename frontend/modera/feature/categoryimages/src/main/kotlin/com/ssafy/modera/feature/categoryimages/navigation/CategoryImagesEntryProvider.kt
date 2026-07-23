package com.ssafy.modera.feature.categoryimages.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.AnalyzedImageSummary
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.categoryimages.CategoryImagesScreen
import com.ssafy.modera.feature.imagedetail.navigation.ImageDetailNavKey

fun EntryProviderScope<NavKey>.categoryImagesEntry(
    navigator: Navigator,
) {
    entry<CategoryImagesNavKey> { key ->
        CategoryImagesScreen(
            categoryName = key.categoryName,
            images = previewCategoryImages[key.categoryId] ?: emptyList(),
            onBackClick = navigator::goBack,
            onImageClick = { imageId ->
                navigator.navigate(ImageDetailNavKey(imageId = imageId))
            },
            onDeleteImages = { /* TODO: 이미지 삭제 API 연동 */ },
        )
    }
}

// TODO: 추후 API 연동 시 삭제
private val previewCategoryImages = mapOf(
    3L to listOf(
        AnalyzedImageSummary(
            id = 1L,
            title = "ASCII HACKATHON",
            imageUrl = "https://picsum.photos/seed/hackathon/400/600",
            hashtags = listOf("해커톤", "SW"),
        ),
        AnalyzedImageSummary(
            id = 2L,
            title = "삼성전자 주가 전망 및 투자 분석",
            imageUrl = "https://picsum.photos/seed/stock/400/600",
            hashtags = listOf("주식", "삼성전자"),
        ),
    ),
    7L to listOf(
        AnalyzedImageSummary(
            id = 2L,
            title = "삼성전자 주가 전망 및 투자 분석",
            imageUrl = "https://picsum.photos/seed/stock/400/600",
            hashtags = listOf("주식", "삼성전자"),
        ),
    ),
)
