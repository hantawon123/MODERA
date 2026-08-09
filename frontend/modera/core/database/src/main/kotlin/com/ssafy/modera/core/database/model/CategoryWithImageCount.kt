package com.ssafy.modera.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.ssafy.modera.core.model.category.Category

data class CategoryWithImageCount(
    @Embedded
    val category: CategoryEntity,
    @ColumnInfo(name = "itemCount")
    val itemCount: Int,
)

fun CategoryWithImageCount.asExternalModel(): Category =
    Category(
        id = category.categoryId,
        title = category.name,
        thumbnailUrl = category.thumbnailUrl,
        itemCount = itemCount,
        tags = emptyList(),
        isNew = category.isNew,
    )