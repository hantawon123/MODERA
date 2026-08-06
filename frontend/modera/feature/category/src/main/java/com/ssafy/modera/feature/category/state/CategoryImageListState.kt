package com.ssafy.modera.feature.category.state

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

internal data class CategoryImageListState(
    val images: List<AnalyzedImage> = emptyList(),
    val isInitialLoading: Boolean = false,
    val error: Throwable? = null,
)
