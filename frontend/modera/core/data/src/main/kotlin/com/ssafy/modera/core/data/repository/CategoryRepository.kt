package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.model.Category
import com.ssafy.modera.core.model.CategorySortType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getCategories(
        sortType: CategorySortType = CategorySortType.NAME_ASC,
    ): Flow<List<Category>>
}