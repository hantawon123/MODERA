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
                val resolvedCategoryId = resolveCategoryId(
                    categories = categories,
                    selectedCategoryId = requestState.selectedCategoryId,
                )

                if (resolvedCategoryId == null) {
                    flowOf(
                        CategoryUiState.Success(
                            selectedCategoryId = 0L,
                            selectedCategoryTitle = "",
                            categories = categories.toSheetItems(),
                            analyzedImages = emptyList(),
                            selectedSortType = requestState.selectedSortType,
                            showCategorySheet = requestState.showCategorySheet,
                            showSortPopup = requestState.showSortPopup,
                        ),
                    )
                } else {
                    analyzedImageRepository
                        .getAnalyzedImages(
                            page = FIRST_PAGE,
                            query = AnalyzedImageQuery(
                                categoryId = resolvedCategoryId,
                            ),
                        )
                        .asResult()
                        .map { imagesResult ->
                            when (imagesResult) {
                                Result.Loading -> CategoryUiState.Loading

                                is Result.Error -> CategoryUiState.Error

                                is Result.Success -> {
                                    val selectedCategory = categories.firstOrNull {
                                        it.id == resolvedCategoryId
                                    }

                                    CategoryUiState.Success(
                                        selectedCategoryId = resolvedCategoryId,
                                        selectedCategoryTitle = selectedCategory?.title.orEmpty(),
                                        categories = categories.toSheetItems(),
                                        analyzedImages = imagesResult.data,
                                        selectedSortType = requestState.selectedSortType,
                                        showCategorySheet = requestState.showCategorySheet,
                                        showSortPopup = requestState.showSortPopup,
                                    )
                                }
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
    ): Long? {
        selectedCategoryId?.takeIf { id ->
            categories.any { it.id == id }
        }?.let { return it }

        navCategoryId.value?.takeIf { id ->
            categories.any { it.id == id }
        }?.let { return it }

        return categories.firstOrNull()?.id
    }

    private fun List<Category>.toSheetItems(): List<CategorySheetItem> =
        map { category ->
            CategorySheetItem(
                id = category.id,
                title = category.title,
                itemCount = category.itemCount,
            )
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
