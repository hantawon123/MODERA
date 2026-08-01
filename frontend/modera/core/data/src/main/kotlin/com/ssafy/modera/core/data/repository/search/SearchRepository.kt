package com.ssafy.modera.core.data.repository.search

import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    fun searchSemanticImages(
        query: String,
        page: Int = 0,
        size: Int = 20,
    ): Flow<List<AnalyzedImage>>
}
