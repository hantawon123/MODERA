package com.ssafy.modera.feature.imagedetail

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail

sealed interface ImageDetailUiState {

    data object Loading : ImageDetailUiState

    data class Success(
        val image: AnalyzedImageDetail,
    ) : ImageDetailUiState

    data class Error(
        val exception: Throwable,
    ) : ImageDetailUiState
}