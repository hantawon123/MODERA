package com.ssafy.modera.core.data.repository

import androidx.datastore.core.DataStore
import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.data.mapper.toDomainModel
import com.ssafy.modera.core.data.mapper.toProtoModel
import com.ssafy.modera.core.datastore.proto.CategoriesCache
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.network.model.category.asExternalModel
import com.ssafy.modera.core.network.service.category.CategoryClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCategoryRepository @Inject constructor(
    private val categoryClient: CategoryClient,
    private val categoriesDataStore: DataStore<CategoriesCache>,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoriesDataStore.data
            .map { cache ->
                cache.categoriesList.map { category ->
                    category.toDomainModel()
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun refreshCategories() {
        withContext(ioDispatcher) {
            val previousCategoryIds = categoriesDataStore.data.first()
                .categoriesList
                .map { category -> category.id }
                .toSet()

            val response = categoryClient.fetchCategories(
                sortType = CategorySortType.UPDATED_DESC,
            )

            val categories = response.list.map { categoryResponse ->
                categoryResponse.asExternalModel(
                    isNew = previousCategoryIds.isNotEmpty() &&
                        categoryResponse.categoryId !in previousCategoryIds,
                )
            }

            categoriesDataStore.updateData { current ->
                current.toBuilder()
                    .clearCategories()
                    .addAllCategories(
                        categories.map { category -> category.toProtoModel() },
                    )
                    .build()
            }
        }
    }

    override suspend fun refreshCategoriesIfEmpty() {
        withContext(ioDispatcher) {
            val isEmpty = categoriesDataStore.data.first()
                .categoriesList
                .isEmpty()

            if (isEmpty) {
                refreshCategories()
            }
        }
    }

    override suspend fun clearNewCategoryFlags() {
        withContext(ioDispatcher) {
            categoriesDataStore.updateData { current ->
                if (current.categoriesList.none { category -> category.isNew }) {
                    return@updateData current
                }

                current.toBuilder()
                    .clearCategories()
                    .addAllCategories(
                        current.categoriesList.map { category ->
                            if (category.isNew) {
                                category.toBuilder()
                                    .setIsNew(false)
                                    .build()
                            } else {
                                category
                            }
                        },
                    )
                    .build()
            }
        }
    }
}
