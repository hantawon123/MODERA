package com.ssafy.modera.core.data

import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.DefaultAnalyzedImageRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageCategoryResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageSummaryResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageTagResponse
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImagesResponse
import com.ssafy.modera.core.network.model.analyzedimage.OcrResponse
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
                    AnalyzedImageSummaryResponse(
                        imageId = 1024L,
                        fileName = "Screenshot_20260716_101010.png",
                        title = "C++ 프로그래밍 입문",
                        summary = "C++ 입문서 정보",
                        status = "COMPLETED",
                        favorite = true,
                        thumbnailUrl = "/images/1024.jpg",
                        tags = listOf(
                            AnalyzedImageTagResponse(
                                tagId = 11L,
                                name = "C++",
                            ),
                            AnalyzedImageTagResponse(
                                tagId = 12L,
                                name = "공부",
                            ),
                        ),
                        categories = listOf(
                            AnalyzedImageCategoryResponse(
                                categoryId = 3L,
                                name = "개발",
                            ),
                        ),
                        createdAt = "2026-07-16T06:00:00.000Z",
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
                        "$BASE_URL/images/1024.jpg",
                        image.thumbnailUrl,
                    )

                    assertEquals(
                        listOf("C++", "공부"),
                        image.hashtags,
                    )

                    assertEquals(
                        ImageAnalysisStatus.COMPLETED,
                        image.status,
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
                fileName = "Screenshot_20260723_154210.png",
                contentHash = "test-content-hash",
                status = "COMPLETED",
                favorite = false,
                title = "삼성전자 주가 전망 및 투자 분석",
                summary = "삼성전자 주가와 투자 전망을 분석한 이미지입니다.",
                ocr = OcrResponse(
                    rawText = "삼성전자 주가 전망",
                    refinedText = "삼성전자 주가 전망 및 투자 분석",
                    confidence = 0.96,
                ),
                tags = listOf(
                    AnalyzedImageTagResponse(
                        tagId = 21L,
                        name = "주식",
                    ),
                    AnalyzedImageTagResponse(
                        tagId = 22L,
                        name = "삼성전자",
                    ),
                ),
                categories = listOf(
                    AnalyzedImageCategoryResponse(
                        categoryId = 5L,
                        name = "금융",
                    ),
                ),
                analysisConfidence = 0.94,
                imageUrl = "/images/101/source",
                createdAt = "2026-07-23T06:42:10.000Z",
                uploadedAt = "2026-07-23T06:42:12.000Z",
                updatedAt = "2026-07-23T06:43:01.000Z",
                lastViewedAt = "2026-07-23T07:15:30.000Z",
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
                        "Screenshot_20260723_154210.png",
                        image.fileName,
                    )

                    assertEquals(
                        ImageAnalysisStatus.COMPLETED,
                        image.status,
                    )

                    assertFalse(image.favorite)

                    assertEquals(
                        "삼성전자 주가 전망 및 투자 분석",
                        image.title,
                    )

                    assertEquals(
                        "삼성전자 주가와 투자 전망을 분석한 이미지입니다.",
                        image.summary,
                    )

                    assertEquals(
                        listOf("주식", "삼성전자"),
                        image.tags,
                    )

                    assertEquals(
                        "금융",
                        image.categories.name,
                    )

                    assertEquals(
                        "$BASE_URL/images/101/source",
                        image.imageUrl,
                    )

                    assertEquals(
                        "삼성전자 주가 전망",
                        image.ocr?.rawText,
                    )

                    assertEquals(
                        "삼성전자 주가 전망 및 투자 분석",
                        image.ocr?.refinedText,
                    )

                    assertEquals(
                        0.96,
                        requireNotNull(image.ocr).confidence,
                        0.0,
                    )

                    awaitComplete()
                }

            verify(
                analyzedImageClient,
            ).fetchAnalyzedImageDetail(
                imageId = 101L,
            )
        }

    private companion object {
        const val BASE_URL =
            "https://i15d207.p.ssafy.io"
    }
}