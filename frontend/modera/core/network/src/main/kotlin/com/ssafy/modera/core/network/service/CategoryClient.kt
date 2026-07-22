package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.model.CategorySortType
import com.ssafy.modera.core.network.model.CategoriesResponse
import javax.inject.Inject

class CategoryClient @Inject constructor(
    private val categoryService: CategoryService,
) {
    suspend fun fetchCategories(
        sortType: CategorySortType = CategorySortType.NAME_ASC,
    ): ApiResponse<CategoriesResponse> =
        categoryService.fetchCategories(
            sort = sortType.queryValue,
        )
}