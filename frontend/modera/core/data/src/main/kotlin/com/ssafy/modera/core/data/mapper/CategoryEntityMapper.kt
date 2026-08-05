package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.network.model.category.CategoryResponse
import com.ssafy.modera.core.network.model.category.asExternalModel

fun CategoryResponse.asEntity(
    isNew: Boolean,
): CategoryEntity {
    val category = asExternalModel(
        isNew = isNew,
    )

    return CategoryEntity(
        categoryId = category.id,
        name = category.title,
        thumbnailUrl = category.thumbnailUrl,
        isNew = category.isNew,
    )
}