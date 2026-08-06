package com.ssafy.modera.feature.category.state

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType

internal data class CategoryScreenState(
    val selectedCategoryId: Long? = null,
    val selectedSortType: AnalyzedImageSortType = AnalyzedImageSortType.UPLOADED_DESC,
    val searchQuery: String = "",
    val showCategorySheet: Boolean = false,
    val showSortPopup: Boolean = false,
)
