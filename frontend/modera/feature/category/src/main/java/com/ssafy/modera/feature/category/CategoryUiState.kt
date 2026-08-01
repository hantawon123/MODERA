package com.ssafy.modera.feature.category

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.core.model.category.CategorySortType

sealed interface CategoryUiState {

    data object Loading : CategoryUiState

    data object Error : CategoryUiState

    data class Success(
        val selectedCategoryId: Long,
        val selectedCategoryTitle: String,
        val categories: List<CategorySheetItem>,
        val analyzedImages: List<AnalyzedImage>,
        val selectedSortType: CategorySortType,
        val showCategorySheet: Boolean = false,
        val showSortPopup: Boolean = false,
        val isAllCategorySelected: Boolean = false,
    ) : CategoryUiState
}
