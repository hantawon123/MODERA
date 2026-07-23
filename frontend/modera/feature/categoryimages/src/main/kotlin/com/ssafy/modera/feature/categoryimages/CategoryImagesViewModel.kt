package com.ssafy.modera.feature.categoryimages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(
    assistedFactory = CategoryImagesViewModel.Factory::class,
)
class CategoryImagesViewModel @AssistedInject constructor(
    analyzedImageRepository: AnalyzedImageRepository,
    @Assisted val categoryId: Long,
) : ViewModel() {

    val uiState: StateFlow<CategoryImagesUiState> =
        analyzedImageRepository
            .getAnalyzedImages(
                page = FIRST_PAGE,
                query = AnalyzedImageQuery(
                    categoryId = categoryId,
                ),
            )
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        CategoryImagesUiState.Loading
                    }

                    is Result.Success -> {
                        CategoryImagesUiState.Success(
                            images = result.data,
                        )
                    }

                    is Result.Error -> {
                        CategoryImagesUiState.Error
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CategoryImagesUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            categoryId: Long,
        ): CategoryImagesViewModel
    }

    private companion object {
        const val FIRST_PAGE = 0
    }
}