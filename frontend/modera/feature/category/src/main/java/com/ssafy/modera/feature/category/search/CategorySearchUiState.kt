package com.ssafy.modera.feature.category.search

import com.ssafy.modera.feature.category.CategoryMaterialUiModel

sealed interface CategorySearchUiState {

    data object Loading : CategorySearchUiState

    data class Success(
        val searchQuery: String = "",
        val recentMaterials: List<CategoryMaterialUiModel> = emptyList(),
        val searchResults: List<CategoryMaterialUiModel> = emptyList(),
    ) : CategorySearchUiState {
        val content: Content
            get() = when {
                searchQuery.isNotBlank() -> Content.SearchResults(searchResults)
                recentMaterials.isEmpty() -> Content.RecentEmpty
                else -> Content.RecentMaterials(recentMaterials)
            }
    }

    data class Error(
        val exception: Throwable,
    ) : CategorySearchUiState

    sealed interface Content {
        data object RecentEmpty : Content

        data class RecentMaterials(
            val materials: List<CategoryMaterialUiModel>,
        ) : Content

        data class SearchResults(
            val results: List<CategoryMaterialUiModel>,
        ) : Content
    }
}

internal object CategorySearchDummyData {
    val sampleMaterial = CategoryMaterialUiModel(
        id = 1,
        title = "성심당 케이크 리스트",
        description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
        tags = listOf("기차", "예약", "KTX"),
        imageUrl = "",
        imageCount = 4,
    )

    val recentMaterials = listOf(
        sampleMaterial,
        sampleMaterial.copy(id = 2),
    )

    val allMaterials = List(3) { index ->
        sampleMaterial.copy(id = index + 1)
    }
}
