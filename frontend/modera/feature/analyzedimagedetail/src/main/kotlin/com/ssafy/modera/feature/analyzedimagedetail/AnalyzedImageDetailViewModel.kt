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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@HiltViewModel(
    assistedFactory = AnalyzedImageDetailViewModel.Factory::class,
)
class AnalyzedImageDetailViewModel @AssistedInject constructor(
    private val analyzedImageRepository: AnalyzedImageRepository,
    @Assisted private val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<AnalyzedImageDetailUiState>
        field = MutableStateFlow<AnalyzedImageDetailUiState>(
            AnalyzedImageDetailUiState.Loading,
        )

    init {
        loadAnalyzedImageDetail()
    }

    private fun loadAnalyzedImageDetail() {
        viewModelScope.launch {
            analyzedImageRepository
                .getAnalyzedImageDetail(imageId)
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
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
        }
    }

    fun reanalyzeAnalyzedImage() {
        if (uiState.value !is AnalyzedImageDetailUiState.Success) {
            return
        }

        uiState.value = AnalyzedImageDetailUiState.Reanalyzing

        viewModelScope.launch {
            analyzedImageRepository
                .reanalyzeAnalyzedImage(
                    imageId = imageId,
                )
                .flatMapLatest {
                    analyzedImageRepository.getAnalyzedImageDetail(
                        imageId = imageId,
                    )
                }
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            AnalyzedImageDetailUiState.Reanalyzing
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
        }
    }

    fun deleteAnalyzedImage(
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            analyzedImageRepository
                .deleteAnalyzedImage(
                    imageId = imageId,
                )
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> {
                            uiState.value = AnalyzedImageDetailUiState.Loading
                        }

                        is Result.Success -> {
                            onDeleted()
                        }

                        is Result.Error -> {
                            uiState.value =
                                AnalyzedImageDetailUiState.Error(
                                    exception = result.exception,
                                )
                        }
                    }
                }
        }
    }
    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): AnalyzedImageDetailViewModel
    }
}