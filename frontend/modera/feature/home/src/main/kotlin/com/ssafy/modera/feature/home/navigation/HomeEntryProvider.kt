package com.ssafy.modera.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.home.HomeScreen
import com.ssafy.modera.feature.home.LocalHomeAnalysisState

fun EntryProviderScope<NavKey>.homeEntry(
    navigator: Navigator,
) {
    entry<HomeNavKey> {
        val analysisState = LocalHomeAnalysisState.current

        // TODO : 추후 더미데이터 삭제
        val previewCategories = listOf(
            Category(
                id = 1,
                title = "쇼핑",
                thumbnailUrl = "https://picsum.photos/seed/shopping1/300/300",
                itemCount = 12,
                tags = listOf("coor", "무신사", "쿠팡"),
            ),
            Category(
                id = 2,
                title = "스포츠",
                thumbnailUrl = "https://picsum.photos/seed/sports1/300/300",
                itemCount = 267,
                tags = listOf("축구", "야구장", "축구선수"),
            ),
            Category(
                id = 3,
                title = "대회/공모전",
                thumbnailUrl = "https://picsum.photos/seed/contest1/300/300",
                itemCount = 3_846,
                tags = listOf("해커톤", "아이디어톤"),
            ),
            Category(
                id = 4,
                title = "음식",
                thumbnailUrl = "https://picsum.photos/seed/food1/300/300",
                itemCount = 9_999,
                tags = listOf("한식", "디저트", "두바이초콜릿"),
            ),
            Category(
                id = 5,
                title = "공부",
                thumbnailUrl = "https://picsum.photos/seed/study1/300/300",
                itemCount = 24,
                tags = listOf("코딩", "자격증", "알고리즘"),
            ),
            Category(
                id = 6,
                title = "여행",
                thumbnailUrl = "https://picsum.photos/seed/travel1/300/300",
                itemCount = 84,
                tags = listOf("제주도", "숙소", "맛집"),
            ),
            Category(
                id = 7,
                title = "금융",
                thumbnailUrl = "https://picsum.photos/seed/finance1/300/300",
                itemCount = 31,
                tags = listOf("주식", "삼성전자", "경제"),
            ),
            Category(
                id = 8,
                title = "일정",
                thumbnailUrl = "https://picsum.photos/seed/schedule1/300/300",
                itemCount = 18,
                tags = listOf("약속", "회의", "공연"),
            ),
        )

        HomeScreen(
            categories = previewCategories,
            selectedSortType = CategorySortType.NAME_ASC,
            onSortTypeChange = {},
            onCategoryClick = {},
            showAnalysisBanner = analysisState.showBanner,
            analysisImageCount = analysisState.imageCount,
            onDismissAnalysisBanner = analysisState::dismissBanner,
        )
    }
}
