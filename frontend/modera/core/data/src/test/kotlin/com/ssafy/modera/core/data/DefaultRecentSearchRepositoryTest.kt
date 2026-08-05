package com.ssafy.modera.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.search.DefaultRecentSearchRepository
import com.ssafy.modera.core.data.repository.search.RecentSearchRepository
import com.ssafy.modera.core.datastore.RecentSearchesSerializer
import com.ssafy.modera.core.datastore.proto.RecentSearches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class DefaultRecentSearchRepositoryTest {

    private lateinit var dataStore: DataStore<RecentSearches>
    private lateinit var repository: RecentSearchRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        dataStore = DataStoreFactory.create(
            serializer = RecentSearchesSerializer,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            produceFile = { File.createTempFile("recent_searches", ".pb") },
        )

        repository = DefaultRecentSearchRepository(
            recentSearchesDataStore = dataStore,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun addRecentSearchQuery_keepsMostRecentFirst() = runTest(testDispatcher) {
        repository.addRecentSearchQuery("케이크")
        repository.addRecentSearchQuery("KTX")

        repository.recentSearchQueries.test {
            assertEquals(listOf("KTX", "케이크"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addRecentSearchQuery_movesExistingQueryToFront() = runTest(testDispatcher) {
        repository.addRecentSearchQuery("케이크")
        repository.addRecentSearchQuery("KTX")
        repository.addRecentSearchQuery("케이크")

        repository.recentSearchQueries.test {
            assertEquals(listOf("케이크", "KTX"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addRecentSearchQuery_keepsMaximumTenQueries() = runTest(testDispatcher) {
        repeat(11) { index ->
            repository.addRecentSearchQuery("검색어$index")
        }

        repository.recentSearchQueries.test {
            val queries = awaitItem()
            assertEquals(10, queries.size)
            assertEquals("검색어10", queries.first())
            assertEquals("검색어1", queries.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeRecentSearchQuery_deletesSelectedQuery() = runTest(testDispatcher) {
        repository.addRecentSearchQuery("케이크")
        repository.addRecentSearchQuery("KTX")
        repository.removeRecentSearchQuery("케이크")

        repository.recentSearchQueries.test {
            assertEquals(listOf("KTX"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearRecentSearchQueries_removesAllQueries() = runTest(testDispatcher) {
        repository.addRecentSearchQuery("케이크")
        repository.addRecentSearchQuery("KTX")

        repository.clearRecentSearchQueries()

        repository.recentSearchQueries.test {
            assertEquals(emptyList<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addRecentSearchQuery_ignoresBlankQuery() = runTest(testDispatcher) {
        repository.addRecentSearchQuery("   ")

        repository.recentSearchQueries.test {
            assertEquals(emptyList<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
