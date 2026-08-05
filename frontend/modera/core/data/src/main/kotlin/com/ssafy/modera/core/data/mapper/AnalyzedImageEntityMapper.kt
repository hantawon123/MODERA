package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import java.time.Instant

fun AnalyzedImageDetailResponse.asEntity(): AnalyzedImageEntity =
    AnalyzedImageEntity(
        imageId = imageId,
        categoryId = categoryId,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl.orEmpty(),
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