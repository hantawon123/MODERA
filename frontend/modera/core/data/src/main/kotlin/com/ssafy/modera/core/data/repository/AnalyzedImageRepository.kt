package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary
import kotlinx.coroutines.flow.Flow

interface AnalyzedImageRepository {

    fun getAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery = AnalyzedImageQuery(),
    ): Flow<List<AnalyzedImageSummary>>

    fun getAnalyzedImageDetail(
        imageId: Long,
    ): Flow<AnalyzedImageDetail>
}