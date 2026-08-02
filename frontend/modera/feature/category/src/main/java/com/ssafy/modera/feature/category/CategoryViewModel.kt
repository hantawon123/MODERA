package com.ssafy.modera.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType
import com.ssafy.modera.feature.category.state.CategoryImageListState
import com.ssafy.modera.feature.category.state.CategoryImageListState.Companion.FIRST_PAGE
import com.ssafy.modera.feature.category.state.CategoryImageListState.Companion.PAGE_SIZE
import com.ssafy.modera.feature.category.state.CategoryScreenState
import com.ssafy.modera.feature.category.state.buildCategoryUiState
import com.ssafy.modera.feature.category.state.resolveCategoryId
import com.ssafy.modera.feature.category.state.toQueryCategoryId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val analyzedImageRepository: AnalyzedImageRepository,
) : ViewModel() {

    private val navCategoryId = MutableStateFlow<Long?>(null)
    private val screenState = MutableStateFlow(CategoryScreenState())
    private val imageListState = MutableStateFlow(CategoryImageListState())
    private var imageLoadGeneration = 0

    private val categoriesResult = categoryRepository
        .observeCategories()
        .asResult()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Result.Loading,
        )

    private val imageQuery = combine(
        categoriesResult,
        screenState,
    ) { categories, screen ->
        when (categories) {
            is Result.Success -> {
                val resolvedCategoryId = resolveCategoryId(
                    categories = categories.data,
                    selectedCategoryId = screen.selectedCategoryId,
                    navCategoryId = navCategoryId.value,
                )

                AnalyzedImageQuery(
                    categoryId = resolvedCategoryId.toQueryCategoryId(),
                    sort = screen.selectedSortType,
                )
            }

            else -> null
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<CategoryUiState> = combine(
        categoriesResult,
        screenState,
        imageListState,
        navCategoryId,
    ) { categoriesResult, screen, images, navCategoryId ->
        buildCategoryUiState(
            categoriesResult = categoriesResult,
            screenState = screen,
            imageListState = images,
            navCategoryId = navCategoryId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState.Loading,
    )

    init {
        viewModelScope.launch {
            categoryRepository.refreshCategoriesIfEmpty()
        }

        viewModelScope.launch {
            imageQuery.collect { query ->
                if (query != null) {
                    loadInitialPage(query)
                }
            }
        }
    }

    fun initialize(
        selectedCategoryId: Long?,
    ) {
        navCategoryId.value = selectedCategoryId
    }

    fun onCategoryTitleClick() {
        viewModelScope.launch {
            categoryRepository.clearNewCategoryFlags()
        }
        screenState.update { it.copy(showCategorySheet = true) }
    }

    fun onCategorySheetDismiss() {
        screenState.update { it.copy(showCategorySheet = false) }
    }

    fun onCategorySelect(
        categoryId: Long,
    ) {
        screenState.update {
            it.copy(
                selectedCategoryId = categoryId,
                showCategorySheet = false,
            )
        }
    }

    fun onSortClick() {
        screenState.update { it.copy(showSortPopup = true) }
    }

    fun onSortPopupDismiss() {
        screenState.update { it.copy(showSortPopup = false) }
    }

    fun onSortTypeSelect(
        sortType: AnalyzedImageSortType,
    ) {
        screenState.update {
            it.copy(
                selectedSortType = sortType,
                showSortPopup = false,
            )
        }
    }

    fun loadNextPage() {
        val query = imageQuery.value ?: return
        val currentState = imageListState.value

        if (
            !currentState.hasNext ||
            currentState.isInitialLoading ||
            currentState.isLoadingMore
        ) {
            return
        }

        viewModelScope.launch {
            val generation = imageLoadGeneration
            imageListState.update { it.copy(isLoadingMore = true) }

            runCatching {
                analyzedImageRepository
                    .getAnalyzedImages(
                        page = currentState.nextPage,
                        query = query,
                    )
                    .first()
            }.onSuccess { images ->
                if (generation != imageLoadGeneration) {
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
                if (generation != imageLoadGeneration) {
                    return@onFailure
                }

                imageListState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private suspend fun loadInitialPage(
        query: AnalyzedImageQuery,
    ) {
        val generation = ++imageLoadGeneration
        imageListState.value = CategoryImageListState(isInitialLoading = true)

        runCatching {
            analyzedImageRepository
                .getAnalyzedImages(
                    page = FIRST_PAGE,
                    query = query,
                )
                .first()
        }.onSuccess { images ->
            if (generation != imageLoadGeneration) {
                return@onSuccess
            }

            imageListState.value = CategoryImageListState(
                images = images,
                hasNext = images.size >= PAGE_SIZE,
                nextPage = FIRST_PAGE + 1,
                isInitialLoading = false,
            )
        }.onFailure { error ->
            if (generation != imageLoadGeneration) {
                return@onFailure
            }

            imageListState.value = CategoryImageListState(
                error = error,
                isInitialLoading = false,
            )
        }
    }
}
