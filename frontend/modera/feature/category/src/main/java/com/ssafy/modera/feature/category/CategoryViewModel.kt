package com.ssafy.modera.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.feature.category.state.CategoryImageListState
import com.ssafy.modera.feature.category.state.CategoryScreenState
import com.ssafy.modera.feature.category.state.buildCategoryUiState
import com.ssafy.modera.feature.category.state.resolveCategoryId
import com.ssafy.modera.feature.category.state.toQueryCategoryId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val analyzedImageRepository: AnalyzedImageRepository,
    private val categoryTabController: CategoryTabController,
) : ViewModel() {

    private val navCategoryId = MutableStateFlow<Long?>(null)
    private val screenState = MutableStateFlow(CategoryScreenState())
    private val hasLoadedImagesOnce = MutableStateFlow(false)

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
        navCategoryId,
    ) { categories, screen, navCategoryId ->
        when (categories) {
            is Result.Success -> {
                val resolvedCategoryId = resolveCategoryId(
                    categories = categories.data,
                    selectedCategoryId = screen.selectedCategoryId,
                    navCategoryId = navCategoryId,
                )

                AnalyzedImageQuery(
                    categoryId = resolvedCategoryId.toQueryCategoryId(),
                    sort = screen.selectedSortType,
                    keyword = screen.searchQuery
                        .trim()
                        .takeIf(String::isNotEmpty),
                )
            }

            else -> null
        }
    }
        .distinctUntilChanged()

    private val imageListState: StateFlow<CategoryImageListState> = imageQuery
        .flatMapLatest { query ->
            if (query == null) {
                flowOf(
                    CategoryImageListState(
                        isInitialLoading = !hasLoadedImagesOnce.value,
                    ),
                )
            } else {
                analyzedImageRepository
                    .getAnalyzedImages(
                        page = 0,
                        query = query,
                    )
                    .map { images ->
                        hasLoadedImagesOnce.value = true
                        CategoryImageListState(
                            images = images,
                        )
                    }
                    .onStart {
                        if (!hasLoadedImagesOnce.value) {
                            emit(
                                CategoryImageListState(
                                    isInitialLoading = true,
                                ),
                            )
                        }
                    }
                    .catch { error ->
                        emit(
                            CategoryImageListState(
                                error = error,
                            ),
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryImageListState(
                isInitialLoading = true,
            ),
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

        categoryTabController.observeShowAll(
            scope = viewModelScope,
            onShowAll = ::showAllCategories,
        )
    }

    fun initialize(
        selectedCategoryId: Long?,
    ) {
        navCategoryId.value = selectedCategoryId
        screenState.update { state ->
            state.copy(
                selectedCategoryId = selectedCategoryId
                    ?: CategorySheetItem.ALL_CATEGORY_ID,
            )
        }
    }

    fun showAllCategories() {
        navCategoryId.value = null
        screenState.update { state ->
            state.copy(
                selectedCategoryId = CategorySheetItem.ALL_CATEGORY_ID,
                showCategorySheet = false,
                showSortPopup = false,
            )
        }
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

    fun onSearchQueryChanged(query: String) {
        screenState.update { it.copy(searchQuery = query) }
    }
}
