package com.ssafy.modera.core.data.repository.search

import androidx.datastore.core.DataStore
import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.datastore.proto.RecentSearches
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultRecentSearchRepository @Inject constructor(
    private val recentSearchesDataStore: DataStore<RecentSearches>,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : RecentSearchRepository {

    override val recentSearchQueries: Flow<List<String>> =
        recentSearchesDataStore.data
            .map { it.queriesList }
            .flowOn(ioDispatcher)

    override suspend fun addRecentSearchQuery(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        recentSearchesDataStore.updateData { current ->
            val updatedQueries = buildList {
                add(trimmedQuery)
                addAll(current.queriesList.filterNot { it == trimmedQuery })
            }.take(MaxRecentSearchQueryCount)

            current.toBuilder()
                .clearQueries()
                .addAllQueries(updatedQueries)
                .build()
        }
    }

    override suspend fun removeRecentSearchQuery(query: String) {
        recentSearchesDataStore.updateData { current ->
            val updatedQueries = current.queriesList.filterNot { it == query }
            if (updatedQueries.size == current.queriesList.size) {
                return@updateData current
            }

            current.toBuilder()
                .clearQueries()
                .addAllQueries(updatedQueries)
                .build()
        }
    }

    override suspend fun clearRecentSearchQueries() {
        recentSearchesDataStore.updateData { current ->
            if (current.queriesList.isEmpty()) {
                return@updateData current
            }

            current.toBuilder()
                .clearQueries()
                .build()
        }
    }

    private companion object {
        const val MaxRecentSearchQueryCount = 10
    }
}
