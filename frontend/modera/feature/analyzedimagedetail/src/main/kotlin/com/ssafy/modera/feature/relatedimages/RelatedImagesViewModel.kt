package com.ssafy.modera.feature.relatedimages

import android.util.Log
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
    assistedFactory = RelatedImagesViewModel.Factory::class,
)
class RelatedImagesViewModel @AssistedInject constructor(
    analyzedImageRepository: AnalyzedImageRepository,
    @Assisted val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<RelatedImagesUiState> =
        analyzedImageRepository
            .getRelatedImages(imageId)
            .asResult()
            .map { result ->
                when (result) {
                    Result.Loading -> {
                        RelatedImagesUiState.Loading
                    }

                    is Result.Success -> {
                        if (result.data.isEmpty()) {
                            RelatedImagesUiState.Empty
                        } else {
                            Log.d("testaaa", "RelatedImagesViewModel - uiState: ${result.data}")
                            RelatedImagesUiState.Success(
                                relatedImages = result.data,
                            )
                        }
                    }

                    is Result.Error -> {
                        RelatedImagesUiState.Error(
                            message = DEFAULT_ERROR_MESSAGE,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RelatedImagesUiState.Loading,
            )

    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): RelatedImagesViewModel
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE =
            "연관 자료를 불러오지 못했습니다."
    }
}