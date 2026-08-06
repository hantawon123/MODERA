package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import java.time.Instant

fun AnalyzedImageDetailResponse.asEntity(): AnalyzedImageEntity =
    AnalyzedImageEntity(
        imageId = imageId,
        categoryId = categoryId,
        imageUrl = "https://i15d207.p.ssafy.io:8443/api/v1/images/${imageId}/file/raw",
        thumbnailUrl = "https://i15d207.p.ssafy.io:8443/api/v1/images/${imageId}/thumbnail/raw",
        title = title,
        summary = summary,
        favorite = favorite,
        tags = tags,
        keyInformation = keyInformation,
        ocrRefinedText = ocrRefinedText.orEmpty(),
        isDocumented = isDocumented,
        isCalendared = isCalendared,
        updatedAt = updatedAt
            ?.let(Instant::parse)
            ?.toEpochMilli()
            ?: 0L,
    )

fun AnalyzedImageDetailResponse.asCategoryEntity(): CategoryEntity =
    CategoryEntity(
        categoryId = categoryId,
        name = category,
        thumbnailUrl = null,
        isNew = true,
    )