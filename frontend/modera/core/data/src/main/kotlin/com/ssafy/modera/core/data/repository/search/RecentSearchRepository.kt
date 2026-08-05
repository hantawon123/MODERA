package com.ssafy.modera.core.data.repository.search

import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {

    val recentSearchQueries: Flow<List<String>>

    suspend fun addRecentSearchQuery(query: String)

    suspend fun removeRecentSearchQuery(query: String)

    suspend fun clearRecentSearchQueries()
}
