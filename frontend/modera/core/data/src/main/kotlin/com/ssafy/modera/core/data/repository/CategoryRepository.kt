package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.model.category.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    suspend fun refreshCategories()

    suspend fun refreshCategoriesIfEmpty()

    suspend fun clearNewCategoryFlags()
}
