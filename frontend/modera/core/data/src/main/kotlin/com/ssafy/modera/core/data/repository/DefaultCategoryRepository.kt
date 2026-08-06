package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.data.mapper.asEntity
import com.ssafy.modera.core.database.dao.CategoryDao
import com.ssafy.modera.core.database.model.asExternalModel
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.network.service.category.CategoryClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCategoryRepository @Inject constructor(
    private val categoryClient: CategoryClient,
    private val categoryDao: CategoryDao,
    @param:Dispatcher(ModeraDispatcher.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao
            .getCategoriesWithImageCount()
            .map { categories ->
                categories.map { category ->
                    category.asExternalModel()
                }
            }

    override suspend fun refreshCategories() {
        withContext(ioDispatcher) {
            val previousCategories = categoryDao
                .getCategoryEntities()
                .associateBy { category ->
                    category.categoryId
                }

            val hadPreviousCategories = previousCategories.isNotEmpty()

            val response = categoryClient.fetchCategories(
                sortType = CategorySortType.UPDATED_DESC,
            )

            val entities = response.list.map { categoryResponse ->
                val previousCategory =
                    previousCategories[categoryResponse.categoryId]

                categoryResponse.asEntity(
                    isNew = previousCategory?.isNew
                        ?: hadPreviousCategories,
                )
            }

            categoryDao.upsertCategories(
                entities = entities,
            )
        }
    }

    override suspend fun refreshCategoriesIfEmpty() {
        withContext(ioDispatcher) {
            if (categoryDao.getCategoryCount() == 0) {
                refreshCategories()
            }
        }
    }

    override suspend fun clearNewCategoryFlags() {
        withContext(ioDispatcher) {
            categoryDao.clearNewCategoryFlags()
        }
    }
}