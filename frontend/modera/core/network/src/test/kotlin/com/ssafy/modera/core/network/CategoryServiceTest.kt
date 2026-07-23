package com.ssafy.modera.core.network

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.service.CategoryService
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class CategoryServiceTest : ApiAbstract<CategoryService>() {

    private lateinit var service: CategoryService

    @Before
    fun setUp() {
        service = createService(CategoryService::class.java)
    }

    @Test
    fun fetchCategoriesFromNetworkTest() = runTest {
        enqueueResponse("CategoriesResponse.json")

        val response = service.fetchCategories(
            sort = "name,asc",
        )

        val data = (response as ApiResponse.Success).data

        assertThat(data.list.size, `is`(2))
        assertThat(data.list.first().categoryId, `is`(3L))
        assertThat(data.list.first().name, `is`("공부"))
        assertThat(data.list.first().tags.first().name, `is`("C++"))
    }
}