package com.ssafy.modera.core.data.repository

import com.ssafy.modera.core.model.image.RegisterImage
import com.ssafy.modera.core.model.image.RegisterImagesResult
import kotlinx.coroutines.flow.Flow

interface ImageRepository {

    fun registerImages(
        images: List<RegisterImage>,
    ): Flow<RegisterImagesResult>
}
