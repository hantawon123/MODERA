package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.network.BuildConfig

import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.network.model.category.CategoryResponse

fun CategoryResponse.asEntity(
    isNew: Boolean,
): CategoryEntity =
    CategoryEntity(
        categoryId = categoryId,
        name = name,
        thumbnailUrl = "${BuildConfig.API_BASE_URL}api/v1/categories/${categoryId}/thumbnail",
        isNew = isNew,
    )
