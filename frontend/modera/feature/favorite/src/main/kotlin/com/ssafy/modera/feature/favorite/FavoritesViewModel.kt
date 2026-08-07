package com.ssafy.modera.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.analyzedImage.AnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    analyzedImageRepository: AnalyzedImageRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> =
        analyzedImageRepository
            .getAnalyzedImages(
                page = 0,
                query = AnalyzedImageQuery(
                    favorite = true,
                ),
            )
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> FavoritesUiState.Loading

                    is Result.Success -> FavoritesUiState.Success(
                        favorites = result.data,
                    )

                    is Result.Error -> FavoritesUiState.Error
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FavoritesUiState.Loading,
            )
}
