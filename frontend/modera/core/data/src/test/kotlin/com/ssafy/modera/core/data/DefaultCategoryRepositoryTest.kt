package com.ssafy.modera.core.data

import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.DefaultCategoryRepository
import com.ssafy.modera.core.model.CategorySortType
import com.ssafy.modera.core.network.model.CategoriesResponse
import com.ssafy.modera.core.network.model.CategoryResponse
import com.ssafy.modera.core.network.model.CategoryTagResponse
import com.ssafy.modera.core.network.service.CategoryClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultCategoryRepositoryTest {

    private lateinit var categoryClient: CategoryClient
    private lateinit var repository: CategoryRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        categoryClient = mock()

        repository = DefaultCategoryRepository(
            categoryClient = categoryClient,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun getCategoriesReturnsMappedCategories() = runTest(testDispatcher) {
        val response = CategoriesResponse(
            list = listOf(
                CategoryResponse(
                    categoryId = 3L,
                    name = "공부",
                    thumbnailUrl = "https://cdn.example.com/study.jpg",
                    imageCount = 42,
                    tags = listOf(
                        CategoryTagResponse(
                            tagId = 11L,
                            name = "C++",
                            imageCount = 14,
                        ),
                    ),
                    updatedAt = "2026-07-17T06:00:00.000Z",
                ),
            ),
        )

        whenever(
            categoryClient.fetchCategories(CategorySortType.NAME_ASC),
        ).thenReturn(response)

        repository
            .getCategories(CategorySortType.NAME_ASC)
            .test {
                val categories = awaitItem()
                val category = categories.first()

                assertEquals(1, categories.size)
                assertEquals(3L, category.id)
                assertEquals("공부", category.title)
                assertEquals(
                    "https://cdn.example.com/study.jpg",
                    category.thumbnailUrl,
                )
                assertEquals(42, category.itemCount)
                assertEquals(listOf("C++"), category.tags)

                awaitComplete()
            }

        verify(categoryClient).fetchCategories(
            CategorySortType.NAME_ASC,
        )
    }
}