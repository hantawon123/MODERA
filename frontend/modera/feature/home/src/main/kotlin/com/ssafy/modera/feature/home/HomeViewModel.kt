package com.ssafy.modera.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.model.category.CategorySortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val selectedSortType =
        MutableStateFlow(CategorySortType.NAME_ASC)

    /**
     * 값이 증가할 때마다 카테고리 목록을 다시 요청한다.
     */
    private val refreshKey = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> =
        combine(
            selectedSortType,
            refreshKey,
        ) { sortType, _ ->
            sortType
        }
            .flatMapLatest { sortType ->
                categoryRepository
                    .getCategories(sortType)
                    .asResult()
                    .map { result ->
                        when (result) {
                            is Result.Success -> {
                                HomeUiState.Success(
                                    categories = result.data,
                                    selectedSortType = sortType,
                                )
                            }

                            is Result.Error -> {
                                HomeUiState.Error(
                                    exception = result.exception,
                                    selectedSortType = sortType,
                                )
                            }

                            Result.Loading -> {
                                HomeUiState.Loading(
                                    selectedSortType = sortType,
                                )
                            }
                        }
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState.Loading(
                    selectedSortType = CategorySortType.NAME_ASC,
                ),
            )

    fun updateSortType(sortType: CategorySortType) {
        selectedSortType.value = sortType
    }

    fun refreshCategories() {
        refreshKey.update { current ->
            current + 1
        }
    }
}