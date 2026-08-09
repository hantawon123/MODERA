package com.ssafy.modera.feature.category.state

import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.feature.category.CategoryUiState

internal fun buildCategoryUiState(
    categoriesResult: Result<List<Category>>,
    screenState: CategoryScreenState,
    imageListState: CategoryImageListState,
    navCategoryId: Long?,
): CategoryUiState {
    when (categoriesResult) {
        Result.Loading -> return CategoryUiState.Loading
        is Result.Error -> return CategoryUiState.Error
        is Result.Success -> {
            if (imageListState.isInitialLoading) {
                return CategoryUiState.Loading
            }

            if (imageListState.error != null) {
                return CategoryUiState.Error
            }

            val categories = categoriesResult.data
            val sheetItems = categories.toSheetItems()
            val resolvedCategoryId = resolveCategoryId(
                categories = categories,
                selectedCategoryId = screenState.selectedCategoryId,
                navCategoryId = navCategoryId,
            )
            val selectedCategoryItem = sheetItems.firstOrNull {
                it.id == resolvedCategoryId
            }

            val isSearching = screenState.searchQuery.isNotBlank()
            val totalImageCount = when {
                isSearching -> imageListState.images.size.toLong()
                else -> selectedCategoryItem?.itemCount?.toLong()
                    ?: imageListState.images.size.toLong()
            }

            return CategoryUiState.Success(
                selectedCategoryId = resolvedCategoryId,
                selectedCategoryTitle = selectedCategoryItem?.title.orEmpty(),
                categories = sheetItems,
                analyzedImages = imageListState.images,
                totalImageCount = totalImageCount,
                selectedSortType = screenState.selectedSortType,
                searchQuery = screenState.searchQuery,
                showCategorySheet = screenState.showCategorySheet,
                showSortPopup = screenState.showSortPopup,
                isAllCategorySelected = selectedCategoryItem?.isAll == true,
            )
        }
    }
}

internal fun resolveCategoryId(
    categories: List<Category>,
    selectedCategoryId: Long?,
    navCategoryId: Long?,
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

    navCategoryId
        ?.takeIf(Long::isValidSelection)
        ?.let { return it }

    return CategorySheetItem.ALL_CATEGORY_ID
}

internal fun Long.toQueryCategoryId(): Long? =
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
                isNew = category.isNew,
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
