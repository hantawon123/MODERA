package com.ssafy.modera.feature.home

import com.ssafy.modera.core.model.category.Category

/* TODO : 추후 도메인으로 분리*/
data class SearchMaterialResult(
    val id: Int,
    val title: String,
    val description: String,
    val tags: List<String>,
    val imageUrl: String? = null,
)

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val categories: List<Category>,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val recentSearchTerms: List<String> = HomeSearchDummyData.recentSearchTerms,
        val searchResults: List<SearchMaterialResult> = emptyList(),
        val isShowingSearchResults: Boolean = false,
        val isSearchLoading: Boolean = false,
    ) : HomeUiState

    data class Error(
        val exception: Throwable,
    ) : HomeUiState
}

internal object HomeSearchDummyData {
    val recentSearchTerms = listOf(
        "성심당 케이크",
        "KTX 예약",
        "주말 브런치 레시피",
    )

    val searchResults = listOf(
        SearchMaterialResult(
            id = 1,
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("음식", "케이크", "성심당"),
            imageUrl = "",
        ),
        SearchMaterialResult(
            id = 2,
            title = "KTX SRT 예약 확인",
            description = "서울역 → 대전역 7월 30일 09:00 출발, 좌석 5A.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
        ),
        SearchMaterialResult(
            id = 3,
            title = "주말 브런치 레시피",
            description = "에그 베네딕트와 팬케이크 레시피 모음.",
            tags = listOf("음식", "레시피"),
            imageUrl = null,
        ),
    )
}
