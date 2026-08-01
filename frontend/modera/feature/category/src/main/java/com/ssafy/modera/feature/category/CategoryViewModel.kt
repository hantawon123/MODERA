package com.ssafy.modera.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.core.model.category.CategorySortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val analyzedImageRepository: AnalyzedImageRepository,
) : ViewModel() {

    private val navCategoryId = MutableStateFlow<Long?>(null)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val selectedSortType = MutableStateFlow(CategorySortType.UPDATED_DESC)
    private val showCategorySheet = MutableStateFlow(false)
    private val showSortPopup = MutableStateFlow(false)

    private val categoriesResult = categoryRepository
        .getCategories(CategorySortType.NAME_ASC)
        .asResult()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Result.Loading,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoryUiState> = combine(
        categoriesResult,
        selectedCategoryId,
        selectedSortType,
        showCategorySheet,
        showSortPopup,
    ) { categories, categoryId, sortType, categorySheetVisible, sortPopupVisible ->
        CategoryRequestState(
            categoriesResult = categories,
            selectedCategoryId = categoryId,
            selectedSortType = sortType,
            showCategorySheet = categorySheetVisible,
            showSortPopup = sortPopupVisible,
        )
    }.flatMapLatest { requestState ->
        when (val categoriesResult = requestState.categoriesResult) {
            Result.Loading -> flowOf(CategoryUiState.Loading)

            is Result.Error -> flowOf(CategoryUiState.Error)

            is Result.Success -> {
                val categories = categoriesResult.data
                val sheetItems = categories.toSheetItems()
                val resolvedCategoryId = resolveCategoryId(
                    categories = categories,
                    selectedCategoryId = requestState.selectedCategoryId,
                )

                analyzedImageRepository
                    .getAnalyzedImages(
                        page = FIRST_PAGE,
                        query = AnalyzedImageQuery(
                            categoryId = resolvedCategoryId.toQueryCategoryId(),
                        ),
                    )
                    .asResult()
                    .map { imagesResult ->
                        when (imagesResult) {
                            Result.Loading -> CategoryUiState.Loading

                            is Result.Error -> CategoryUiState.Error

                            is Result.Success -> {
                                val selectedCategoryItem = sheetItems.firstOrNull {
                                    it.id == resolvedCategoryId
                                }

                                CategoryUiState.Success(
                                    selectedCategoryId = resolvedCategoryId,
                                    selectedCategoryTitle = selectedCategoryItem?.title.orEmpty(),
                                    categories = sheetItems,
                                    analyzedImages = imagesResult.data,
                                    selectedSortType = requestState.selectedSortType,
                                    showCategorySheet = requestState.showCategorySheet,
                                    showSortPopup = requestState.showSortPopup,
                                    isAllCategorySelected = selectedCategoryItem?.isAll == true,
                                )
                            }
                        }
                    }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState.Loading,
    )

    fun initialize(
        selectedCategoryId: Long?,
    ) {
        navCategoryId.value = selectedCategoryId
    }

    fun onCategoryTitleClick() {
        showCategorySheet.update { true }
    }

    fun onCategorySheetDismiss() {
        showCategorySheet.update { false }
    }

    fun onCategorySelect(
        categoryId: Long,
    ) {
        selectedCategoryId.value = categoryId
        showCategorySheet.update { false }
    }

    fun onSortClick() {
        showSortPopup.update { true }
    }

    fun onSortPopupDismiss() {
        showSortPopup.update { false }
    }

    fun onSortTypeSelect(
        sortType: CategorySortType,
    ) {
        selectedSortType.value = sortType
        showSortPopup.update { false }
    }

    private fun resolveCategoryId(
        categories: List<Category>,
        selectedCategoryId: Long?,
    ): Long {
        val visibleCategoryIds = categories
            .filter { category -> category.itemCount > 0 }
            .map(Category::id)
            .toSet()

        fun Long.isValidSelection(): Boolean =
            this == CategorySheetItem.ALL_CATEGORY_ID || this in visibleCategoryIds

        selectedCategoryId
            ?.takeIf(Long::isValidSelection)
            ?.let { return it }

        navCategoryId.value
            ?.takeIf(Long::isValidSelection)
            ?.let { return it }

        return CategorySheetItem.ALL_CATEGORY_ID
    }

    private fun Long.toQueryCategoryId(): Long? =
        takeUnless { it == CategorySheetItem.ALL_CATEGORY_ID }

    private fun List<Category>.toSheetItems(): List<CategorySheetItem> {
        val categoryItems = filter { category -> category.itemCount > 0 }
            .sortedWith(
                compareByDescending(Category::itemCount)
                    .thenBy(Category::title),
            )
            .map { category ->
                CategorySheetItem(
                    id = category.id,
                    title = category.title,
                    itemCount = category.itemCount,
                )
            }

        return buildList {
            add(
                CategorySheetItem(
                    id = CategorySheetItem.ALL_CATEGORY_ID,
                    title = "",
                    itemCount = sumOf(Category::itemCount),
                    isAll = true,
                ),
            )
            addAll(categoryItems)
        }
    }

    private data class CategoryRequestState(
        val categoriesResult: Result<List<Category>>,
        val selectedCategoryId: Long?,
        val selectedSortType: CategorySortType,
        val showCategorySheet: Boolean,
        val showSortPopup: Boolean,
    )

    private companion object {
        const val FIRST_PAGE = 0
    }
}
