package com.ssafy.modera.feature.home

import android.util.Log
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val selectedSortType =
        MutableStateFlow(CategorySortType.NAME_ASC)

    val uiState: StateFlow<HomeUiState> =
        selectedSortType
            .flatMapLatest { sortType ->
                categoryRepository
                    .getCategories(sortType)
                    .asResult()
                    .map { result ->
                        when (result) {
                            is Result.Success -> {
                                Log.d(
                                    "testaaa", "${
                                        result.data.map {
                                            it.thumbnailUrl
                                        }
                                    }"
                                )
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

                            Result.Loading -> HomeUiState.Loading(
                                selectedSortType = sortType,
                            )
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
}