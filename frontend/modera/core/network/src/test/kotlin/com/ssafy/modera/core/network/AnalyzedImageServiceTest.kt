package com.ssafy.modera.core.network

import com.skydoves.sandwich.ApiResponse
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

        val response = service.fetchAnalyzedImages()

        val data = (response as ApiResponse.Success).data
        val image = data.list.first()

        assertThat(data.list.size, `is`(1))
        assertThat(image.imageId, `is`(1024L))
        assertThat(image.title, `is`("C++ 프로그래밍 입문"))
        assertThat(image.status, `is`("COMPLETED"))
        assertThat(image.tags.first().name, `is`("C++"))
        assertThat(image.categories.first().name, `is`("공부"))
        assertThat(data.hasNext, `is`(false))
    }
}