package com.ssafy.modera.core.data.repository.search

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.network.model.search.asExternalModel
import com.ssafy.modera.core.network.service.search.SearchClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultSearchRepository @Inject constructor(
    private val searchClient: SearchClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : SearchRepository {

    override fun searchSemanticImages(
        query: String,
        page: Int,
        size: Int,
    ): Flow<List<AnalyzedImage>> = flow {
        val response = searchClient.searchSemanticImages(
            query = query,
            page = page,
            size = size,
        )
        emit(response.list.map { it.asExternalModel() })
    }.flowOn(ioDispatcher)
}
