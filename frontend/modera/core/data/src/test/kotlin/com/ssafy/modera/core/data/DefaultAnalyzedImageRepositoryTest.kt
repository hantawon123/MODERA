package com.ssafy.modera.core.data

import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.DefaultAnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultAnalyzedImageRepositoryTest {

    private lateinit var analyzedImageClient: AnalyzedImageClient
    private lateinit var repository: AnalyzedImageRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        analyzedImageClient = mock()

        repository = DefaultAnalyzedImageRepository(
            analyzedImageClient = analyzedImageClient,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun getAnalyzedImagesReturnsMappedImageSummaries() =
        runTest(testDispatcher) {
            val query = AnalyzedImageQuery(
                statuses = setOf(
                    ImageAnalysisStatus.COMPLETED,
                ),
            )

            val response = AnalyzedImagesResponse(
                list = listOf(
                    AnalyzedImageResponse(
                        imageId = 1024L,
                        title = "C++ 프로그래밍 입문",
                        summary = "C++ 입문서 정보",
                        favorite = true,
                        thumbnailUrl = "/images/1024.jpg",
                        tags = listOf("C++", "공부"),
                        createdAt = "2026-07-16T06:00:00.000Z",
                        category = "개발",
                    ),
                ),
                page = 0,
                size = 20,
                totalElements = 1L,
                totalPages = 1,
                hasNext = false,
            )

            whenever(
                analyzedImageClient.fetchAnalyzedImages(
                    page = 0,
                    query = query,
                ),
            ).thenReturn(response)

            repository
                .getAnalyzedImages(
                    page = 0,
                    query = query,
                )
                .test {
                    val images = awaitItem()
                    val image = images.first()

                    assertEquals(
                        1,
                        images.size,
                    )

                    assertEquals(
                        1024L,
                        image.id,
                    )

                    assertEquals(
                        "C++ 프로그래밍 입문",
                        image.title,
                    )

                    assertEquals(
                        "/images/1024.jpg",
                        image.thumbnailUrl,
                    )

                    assertEquals(
                        listOf("C++", "공부"),
                        image.hashtags,
                    )

                    assertTrue(image.favorite)

                    awaitComplete()
                }

            verify(
                analyzedImageClient,
            ).fetchAnalyzedImages(
                page = 0,
                query = query,
            )
        }

    @Test
    fun getAnalyzedImageDetailReturnsMappedImageDetail() =
        runTest(testDispatcher) {
            val response = AnalyzedImageDetailResponse(
                imageId = 101L,
                imageUrl = "/images/101/source",
                thumbnailUrl = "/images/101/thumbnail",
                title = "삼성전자 주가 전망 및 투자 분석",
                favorite = false,
                summary = "삼성전자 주가와 투자 전망을 분석한 이미지입니다.",
                category = "금융",
                tags = listOf(
                    "주식",
                    "삼성전자",
                ),
                keyInformation = listOf(
                    "삼성전자",
                    "주가 전망",
                    "투자 분석",
                ),
                isDocumented = true,
                isCalendared = false,
            )

            whenever(
                analyzedImageClient.fetchAnalyzedImageDetail(
                    imageId = 101L,
                ),
            ).thenReturn(response)

            repository
                .getAnalyzedImageDetail(
                    imageId = 101L,
                )
                .test {
                    val image = awaitItem()

                    assertEquals(
                        101L,
                        image.id,
                    )

                    assertEquals(
                        "/images/101/source",
                        image.imageUrl,
                    )

                    assertEquals(
                        "/images/101/thumbnail",
                        image.thumbnailUrl,
                    )

                    assertEquals(
                        "삼성전자 주가 전망 및 투자 분석",
                        image.title,
                    )

                    assertFalse(image.favorite)

                    assertEquals(
                        "삼성전자 주가와 투자 전망을 분석한 이미지입니다.",
                        image.summary,
                    )

                    assertEquals(
                        "금융",
                        image.category,
                    )

                    assertEquals(
                        listOf("주식", "삼성전자"),
                        image.tags,
                    )

                    assertEquals(
                        emptyList<String>(),
                        image.extractedTexts,
                    )

                    assertEquals(
                        listOf(
                            "삼성전자",
                            "주가 전망",
                            "투자 분석",
                        ),
                        image.keyInformation,
                    )

                    assertTrue(image.isDocumented)

                    assertFalse(image.isCalendared)

                    assertEquals(
                        0L,
                        image.updatedAt,
                    )

                    awaitComplete()
                }

            verify(
                analyzedImageClient,
            ).fetchAnalyzedImageDetail(
                imageId = 101L,
            )
        }
}