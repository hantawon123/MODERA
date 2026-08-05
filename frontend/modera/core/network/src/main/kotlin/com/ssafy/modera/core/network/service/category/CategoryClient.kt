package com.ssafy.modera.core.network.service.category

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.network.model.category.CategoriesResponse
import javax.inject.Inject

class CategoryClient @Inject constructor(
    private val categoryService: CategoryService,
) {
    suspend fun fetchCategories(
        sortType: CategorySortType = CategorySortType.NAME_ASC,
    ): CategoriesResponse =
        categoryService.fetchCategories(
            sort = sortType.queryValue,
        )
            .getOrThrow()
            .data
}