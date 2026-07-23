package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.Category
import com.ssafy.modera.core.model.CategorySortType
import com.ssafy.modera.core.network.model.asExternalModel
import com.ssafy.modera.core.network.service.CategoryClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultCategoryRepository @Inject constructor(
    private val categoryClient: CategoryClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override fun getCategories(
        sortType: CategorySortType,
    ): Flow<List<Category>> = flow<List<Category>> {
        emit(categoryClient.fetchCategories(sortType).list.map { it.asExternalModel() })
    }.flowOn(ioDispatcher)
}