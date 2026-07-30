package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import kotlinx.coroutines.flow.Flow

interface AnalyzedImageRepository {

    fun getAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery = AnalyzedImageQuery(),
    ): Flow<List<AnalyzedImage>>

    fun getAnalyzedImageDetail(
        imageId: Long,
    ): Flow<AnalyzedImageDetail>

    fun getRelatedImages(
        imageId: Long,
    ): Flow<List<AnalyzedImage>>

    fun reanalyzeAnalyzedImage(imageId: Long): Flow<Unit>

    fun deleteAnalyzedImage(imageId: Long): Flow<Unit>
}