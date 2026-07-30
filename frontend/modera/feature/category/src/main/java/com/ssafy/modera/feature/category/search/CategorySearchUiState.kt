package com.ssafy.modera.feature.category.search

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

sealed interface CategorySearchUiState {

    data object Loading : CategorySearchUiState

    data class Success(
        val searchQuery: String = "",
        val recentAnalyzedImages: List<AnalyzedImage> = emptyList(),
        val searchResults: List<AnalyzedImage> = emptyList(),
    ) : CategorySearchUiState {
        val content: Content
            get() = when {
                searchQuery.isNotBlank() -> Content.SearchResults(searchResults)
                recentAnalyzedImages.isEmpty() -> Content.RecentEmpty
                else -> Content.RecentAnalyzedImages(recentAnalyzedImages)
            }
    }

    data class Error(
        val exception: Throwable,
    ) : CategorySearchUiState

    sealed interface Content {
        data object RecentEmpty : Content

        data class RecentAnalyzedImages(
            val analyzedImages: List<AnalyzedImage>,
        ) : Content

        data class SearchResults(
            val results: List<AnalyzedImage>,
        ) : Content
    }
}

internal object CategorySearchDummyData {
    val sampleMaterial = AnalyzedImage(
        id = 1,
        title = "성심당 케이크 리스트",
        summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
        hashtags = listOf("기차", "예약", "KTX"),
        thumbnailUrl = "",
    )

    val recentMaterials = listOf(
        sampleMaterial,
        sampleMaterial.copy(id = 2),
    )

    val allMaterials = List(3) { index ->
        sampleMaterial.copy(id = (index + 1).toLong())
    }
}
