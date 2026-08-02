package com.ssafy.modera.feature.category

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType
import com.ssafy.modera.core.model.category.CategorySheetItem

sealed interface CategoryUiState {

    data object Loading : CategoryUiState

    data object Error : CategoryUiState

    data class Success(
        val selectedCategoryId: Long,
        val selectedCategoryTitle: String,
        val categories: List<CategorySheetItem>,
        val analyzedImages: List<AnalyzedImage>,
        val totalImageCount: Long,
        val selectedSortType: AnalyzedImageSortType,
        val showCategorySheet: Boolean = false,
        val showSortPopup: Boolean = false,
        val isAllCategorySelected: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasNextPage: Boolean = false,
    ) : CategoryUiState
}
