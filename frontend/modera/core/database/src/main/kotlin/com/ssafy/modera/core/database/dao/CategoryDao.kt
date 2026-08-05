package com.ssafy.modera.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.database.model.CategoryWithImageCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT
            categories.*,
            COUNT(analyzed_images.imageId) AS itemCount
        FROM categories
        LEFT JOIN analyzed_images
            ON categories.categoryId = analyzed_images.categoryId
        GROUP BY categories.categoryId
        """,
    )
    fun getCategoriesWithImageCount(): Flow<List<CategoryWithImageCount>>

    @Query(
        """
        SELECT *
        FROM categories
        """,
    )
    suspend fun getCategoryEntities(): List<CategoryEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM categories
        """,
    )
    suspend fun getCategoryCount(): Int

    @Upsert
    suspend fun upsertCategories(
        entities: List<CategoryEntity>,
    )

    @Query(
        """
        UPDATE categories
        SET isNew = 0
        WHERE isNew = 1
        """,
    )
    suspend fun clearNewCategoryFlags()
}