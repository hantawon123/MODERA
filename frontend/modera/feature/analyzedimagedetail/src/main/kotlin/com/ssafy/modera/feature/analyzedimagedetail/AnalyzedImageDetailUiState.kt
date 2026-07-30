package com.ssafy.modera.feature.analyzedimagedetail

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail

sealed interface AnalyzedImageDetailUiState {

    data object Loading : AnalyzedImageDetailUiState

    data object Reanalyzing : AnalyzedImageDetailUiState

    data class Success(
        val image: AnalyzedImageDetail,
    ) : AnalyzedImageDetailUiState

    data class Error(
        val exception: Throwable,
    ) : AnalyzedImageDetailUiState
}