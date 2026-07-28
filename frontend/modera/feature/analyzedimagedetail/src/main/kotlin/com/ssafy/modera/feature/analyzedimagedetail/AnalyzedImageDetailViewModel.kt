package com.ssafy.modera.feature.analyzedimagedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(
    assistedFactory = AnalyzedImageDetailViewModel.Factory::class,
)
class AnalyzedImageDetailViewModel @AssistedInject constructor(
    analyzedImageRepository: AnalyzedImageRepository,
    @Assisted val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<AnalyzedImageDetailUiState> =
        analyzedImageRepository
            .getAnalyzedImageDetail(imageId)
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        AnalyzedImageDetailUiState.Loading
                    }

                    is Result.Success -> {
                        AnalyzedImageDetailUiState.Success(
                            image = result.data,
                        )
                    }

                    is Result.Error -> {
                        AnalyzedImageDetailUiState.Error(
                            exception = result.exception,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AnalyzedImageDetailUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): AnalyzedImageDetailViewModel
    }
}