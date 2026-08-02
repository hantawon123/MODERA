package com.ssafy.modera.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val analyzedImageRepository: AnalyzedImageRepository,
) : ViewModel() {

    private val imageListState = MutableStateFlow(FavoritesImageListState())
    private var loadGeneration = 0

    private val query = AnalyzedImageQuery(
        favorite = true,
    )

    val uiState: StateFlow<FavoritesUiState> = imageListState
        .map(::buildUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading,
        )

    init {
        viewModelScope.launch {
            loadInitialPage()
        }
    }

    fun loadNextPage() {
        val currentState = imageListState.value

        if (
            !currentState.hasNext ||
            currentState.isInitialLoading ||
            currentState.isLoadingMore
        ) {
            return
        }

        viewModelScope.launch {
            val generation = loadGeneration
            imageListState.update { it.copy(isLoadingMore = true) }

            runCatching {
                analyzedImageRepository
                    .getAnalyzedImages(
                        page = currentState.nextPage,
                        query = query,
                    )
                    .first()
            }.onSuccess { images ->
                if (generation != loadGeneration) {
                    return@onSuccess
                }

                imageListState.update { state ->
                    state.copy(
                        images = state.images + images,
                        hasNext = images.size >= PAGE_SIZE,
                        nextPage = currentState.nextPage + 1,
                        isLoadingMore = false,
                    )
                }
            }.onFailure {
                if (generation != loadGeneration) {
                    return@onFailure
                }

                imageListState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private suspend fun loadInitialPage() {
        val generation = ++loadGeneration
        imageListState.value = FavoritesImageListState(isInitialLoading = true)

        runCatching {
            analyzedImageRepository
                .getAnalyzedImages(
                    page = FIRST_PAGE,
                    query = query,
                )
                .first()
        }.onSuccess { images ->
            if (generation != loadGeneration) {
                return@onSuccess
            }

            imageListState.value = FavoritesImageListState(
                images = images,
                hasNext = images.size >= PAGE_SIZE,
                nextPage = FIRST_PAGE + 1,
                isInitialLoading = false,
            )
        }.onFailure { error ->
            if (generation != loadGeneration) {
                return@onFailure
            }

            imageListState.value = FavoritesImageListState(
                error = error,
                isInitialLoading = false,
            )
        }
    }

    private fun buildUiState(
        state: FavoritesImageListState,
    ): FavoritesUiState {
        if (state.isInitialLoading) {
            return FavoritesUiState.Loading
        }

        if (state.error != null) {
            return FavoritesUiState.Error
        }

        return FavoritesUiState.Success(
            favorites = state.images,
            isLoadingMore = state.isLoadingMore,
            hasNextPage = state.hasNext,
        )
    }

    private data class FavoritesImageListState(
        val images: List<AnalyzedImage> = emptyList(),
        val nextPage: Int = FIRST_PAGE,
        val hasNext: Boolean = false,
        val isInitialLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: Throwable? = null,
    )

    private companion object {
        const val FIRST_PAGE = 0
        const val PAGE_SIZE = 20
    }
}
