package com.ssafy.modera.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
)
data class CategoryEntity(
    @PrimaryKey
    val categoryId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val isNew: Boolean = true,
)