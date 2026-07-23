package com.ssafy.modera.feature.home

import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType

sealed interface HomeUiState {

    val selectedSortType: CategorySortType

    data class Success(
        val categories: List<Category>,
        override val selectedSortType: CategorySortType,
    ) : HomeUiState

    data class Loading(
        override val selectedSortType: CategorySortType,
    ) : HomeUiState

    data class Error(
        val exception: Throwable,
        override val selectedSortType: CategorySortType,
    ) : HomeUiState
}