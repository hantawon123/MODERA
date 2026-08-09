package com.ssafy.modera.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.DefaultCategoryRepository
import com.ssafy.modera.core.datastore.CategoriesCacheSerializer
import com.ssafy.modera.core.datastore.proto.CategoriesCache
import com.ssafy.modera.core.datastore.proto.CategoryProto
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.network.model.category.CategoriesResponse
import com.ssafy.modera.core.network.model.category.CategoryResponse
import com.ssafy.modera.core.network.service.category.CategoryClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultCategoryRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var categoryClient: CategoryClient
    private lateinit var dataStore: DataStore<CategoriesCache>
    private lateinit var repository: CategoryRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        categoryClient = mock()
        dataStore = DataStoreFactory.create(
            serializer = CategoriesCacheSerializer,
            produceFile = {
                temporaryFolder.newFile("categories_cache.pb")
            },
        )

        repository = DefaultCategoryRepository(
            categoryClient = categoryClient,
            ioDispatcher = testDispatcher,
            categoryDao = mock(),
        )
    }

    @Test
    fun observeCategoriesReturnsCachedCategories() = runTest(testDispatcher) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addCategories(
                    CategoryProto.newBuilder()
                        .setId(3L)
                        .setTitle("공부")
                        .setThumbnailUrl("https://example.com/thumb.jpg")
                        .setItemCount(42)
                        .build(),
                )
                .build()
        }

        repository.observeCategories().test {
            val categories = awaitItem()
            val category = categories.first()

            assertEquals(1, categories.size)
            assertEquals(3L, category.id)
            assertEquals("공부", category.title)
            assertEquals("https://example.com/thumb.jpg", category.thumbnailUrl)
            assertEquals(42, category.itemCount)
            assertFalse(category.isNew)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshCategoriesStoresFetchedCategories() = runTest(testDispatcher) {
        whenever(
            categoryClient.fetchCategories(CategorySortType.UPDATED_DESC),
        ).thenReturn(
            CategoriesResponse(
                list = listOf(
                    CategoryResponse(
                        categoryId = 3L,
                        name = "공부",
                        categoryImageUrl = "/thumbnails/1024.jpg",
                        imageCount = 42,
                        latestUpdatedAt = "2026-07-17T06:00:00.000Z",
                    ),
                ),
            ),
        )

        repository.refreshCategories()

        repository.observeCategories().test {
            val categories = awaitItem()
            val category = categories.first()

            assertEquals(1, categories.size)
            assertEquals(3L, category.id)
            assertEquals("공부", category.title)
            assertEquals(
                "https://i15d207.p.ssafy.io:8443/thumbnails/1024.jpg",
                category.thumbnailUrl,
            )
            assertEquals(42, category.itemCount)
            assertFalse(category.isNew)

            cancelAndIgnoreRemainingEvents()
        }

        verify(categoryClient).fetchCategories(CategorySortType.UPDATED_DESC)
    }

    @Test
    fun refreshCategoriesIfEmptyFetchesWhenCacheIsEmpty() = runTest(testDispatcher) {
        whenever(
            categoryClient.fetchCategories(CategorySortType.UPDATED_DESC),
        ).thenReturn(
            CategoriesResponse(
                list = listOf(
                    CategoryResponse(
                        categoryId = 3L,
                        name = "공부",
                        imageCount = 42,
                    ),
                ),
            ),
        )

        repository.refreshCategoriesIfEmpty()

        repository.observeCategories().test {
            val categories = awaitItem()

            assertEquals(1, categories.size)
            assertEquals(3L, categories.first().id)

            cancelAndIgnoreRemainingEvents()
        }

        verify(categoryClient).fetchCategories(CategorySortType.UPDATED_DESC)
    }

    @Test
    fun refreshCategoriesIfEmptySkipsFetchWhenCacheExists() = runTest(testDispatcher) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addCategories(
                    CategoryProto.newBuilder()
                        .setId(3L)
                        .setTitle("공부")
                        .setItemCount(42)
                        .build(),
                )
                .build()
        }

        repository.refreshCategoriesIfEmpty()

        verify(categoryClient, never()).fetchCategories(
            CategorySortType.UPDATED_DESC,
        )
    }

    @Test
    fun refreshCategoriesMarksNewlyCreatedCategories() = runTest(testDispatcher) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addCategories(
                    CategoryProto.newBuilder()
                        .setId(1L)
                        .setTitle("쇼핑")
                        .setItemCount(10)
                        .build(),
                )
                .build()
        }

        whenever(
            categoryClient.fetchCategories(CategorySortType.UPDATED_DESC),
        ).thenReturn(
            CategoriesResponse(
                list = listOf(
                    CategoryResponse(
                        categoryId = 1L,
                        name = "쇼핑",
                        imageCount = 10,
                    ),
                    CategoryResponse(
                        categoryId = 2L,
                        name = "여행",
                        imageCount = 3,
                    ),
                ),
            ),
        )

        repository.refreshCategories()

        repository.observeCategories().test {
            val categories = awaitItem()

            assertEquals(2, categories.size)
            assertFalse(categories.first { it.id == 1L }.isNew)
            assertTrue(categories.first { it.id == 2L }.isNew)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearNewCategoryFlagsRemovesNewBadge() = runTest(testDispatcher) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addCategories(
                    CategoryProto.newBuilder()
                        .setId(2L)
                        .setTitle("여행")
                        .setItemCount(3)
                        .setIsNew(true)
                        .build(),
                )
                .build()
        }

        repository.clearNewCategoryFlags()

        repository.observeCategories().test {
            val categories = awaitItem()

            assertFalse(categories.first().isNew)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
