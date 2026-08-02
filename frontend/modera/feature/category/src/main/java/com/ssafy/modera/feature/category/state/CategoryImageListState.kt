package com.ssafy.modera.feature.category.state

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

internal data class CategoryImageListState(
    val images: List<AnalyzedImage> = emptyList(),
    val nextPage: Int = FIRST_PAGE,
    val hasNext: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
) {
    internal companion object {
        const val FIRST_PAGE = 0
        const val PAGE_SIZE = 20
    }
}
