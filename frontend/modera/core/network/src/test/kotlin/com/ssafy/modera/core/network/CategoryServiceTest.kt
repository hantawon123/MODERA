package com.ssafy.modera.core.network

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.service.category.CategoryService
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
            sort = "NAME_ASC",
        )

        val baseResponse = response.getOrThrow()
        val data = baseResponse.data

        assertThat(baseResponse.code, `is`("T202"))
        assertThat(baseResponse.message, `is`("요청이 성공했습니다."))

        assertThat(data.list.size, `is`(5))
        assertThat(data.list.first().categoryId, `is`(1502052825L))
        assertThat(data.list.first().name, `is`("개발"))
        assertThat(data.list.first().categoryImageUrl, `is`(null as String?))
        assertThat(data.list.first().imageCount, `is`(1))
        assertThat(
            data.list[1].categoryImageUrl,
            `is`("/images/weather.jpg"),
        )
    }
}
