package com.ssafy.modera.core.network

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.service.AnalyzedImageService
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class AnalyzedImageServiceTest : ApiAbstract<AnalyzedImageService>() {

    private lateinit var service: AnalyzedImageService

    @Before
    fun setUp() {
        service = createService(AnalyzedImageService::class.java)
    }

    @Test
    fun fetchAnalyzedImagesFromNetworkTest() = runTest {
        enqueueResponse("AnalyzedImagesResponse.json")

        val response = service.fetchAnalyzedImages(
            favorite = null,
            page = 0,
            size = 20,
            sort = "UPLOADED_DESC",
            keyword = null,
            categoryId = null,
        )

        val baseResponse = response.getOrThrow()
        val data = baseResponse.data
        val image = data.list.first()

        assertThat(baseResponse.code, `is`("SUCCESS"))
        assertThat(baseResponse.message, `is`("요청이 성공했습니다."))

        assertThat(data.list.size, `is`(1))
        assertThat(data.page, `is`(0))
        assertThat(data.size, `is`(20))
        assertThat(data.totalElements, `is`(1L))
        assertThat(data.totalPages, `is`(1))
        assertThat(data.hasNext, `is`(false))

        assertThat(image.imageId, `is`(1024L))
        assertThat(image.title, `is`("C++ 프로그래밍 입문"))
        assertThat(image.tags.first(), `is`("C++"))
        assertThat(image.category, `is`("공부"))
        assertThat(image.isDocumented, `is`(true))
        assertThat(image.isCalendared, `is`(false))
    }

    @Test
    fun fetchAnalyzedImageDetailFromNetworkTest() = runTest {
        enqueueResponse("AnalyzedImageDetailResponse.json")

        val response = service.fetchAnalyzedImageDetail(
            imageId = 101L,
        )

        val baseResponse = response.getOrThrow()
        val image = baseResponse.data

        assertThat(baseResponse.code, `is`("SUCCESS"))
        assertThat(baseResponse.message, `is`("요청이 성공했습니다."))

        assertThat(image.imageId, `is`(101L))
        assertThat(image.favorite, `is`(true))
        assertThat(image.title, `is`("C++ 프로그래밍 입문서 정보"))
        assertThat(image.tags.first(), `is`("C++"))
        assertThat(image.category, `is`("공부"))
        assertThat(
            image.imageUrl,
            `is`("https://example.com/images/101/source"),
        )
    }
}