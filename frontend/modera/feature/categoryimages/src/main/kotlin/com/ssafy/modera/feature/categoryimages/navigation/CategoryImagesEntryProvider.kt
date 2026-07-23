package com.ssafy.modera.feature.categoryimages.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.categoryimages.CategoryImagesScreen

fun EntryProviderScope<NavKey>.categoryImagesEntry(
    navigator: Navigator,
) {
    // Todo: mock data 삭제
    entry<CategoryImagesNavKey> { navKey ->
        CategoryImagesScreen(
            categoryName = "주식${navKey.id}",
            images = List(12) { index ->
                AnalyzedImageSummary(
                    id = index.toLong(),
                    title = "삼성전자 주가 전망 및 투자 분석",
                    imageUrl = "https://picsum.photos/seed/category_$index/400/600",
                    hashtags = listOf(
                        "주식",
                        "삼성전자",
                        "투자",
                    ),
                    status = ImageAnalysisStatus.COMPLETED,
                    favorite = index % 3 == 0,
                )
            },
            onBackClick = {},
            onImageClick = {},
            onDeleteImages = {},
        )
    }
}
