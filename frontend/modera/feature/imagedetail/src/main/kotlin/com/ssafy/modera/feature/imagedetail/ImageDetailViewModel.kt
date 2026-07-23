package com.ssafy.modera.feature.imagedetail

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
    assistedFactory = ImageDetailViewModel.Factory::class,
)
class ImageDetailViewModel @AssistedInject constructor(
    analyzedImageRepository: AnalyzedImageRepository,
    @Assisted val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<ImageDetailUiState> =
        analyzedImageRepository
            .getAnalyzedImageDetail(imageId)
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        ImageDetailUiState.Loading
                    }

                    is Result.Success -> {
                        ImageDetailUiState.Success(
                            image = result.data,
                        )
                    }

                    is Result.Error -> {
                        ImageDetailUiState.Error(
                            exception = result.exception,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ImageDetailUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): ImageDetailViewModel
    }
}