package com.ssafy.modera.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.AnalyzedImageWithCategory
import com.ssafy.modera.core.database.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyzedImageDao {

    @Query(
        """
        SELECT *
        FROM analyzed_images
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:favorite IS NULL OR favorite = :favorite)
          AND (
              :keyword IS NULL
              OR title COLLATE NOCASE LIKE '%' || :keyword || '%'
              OR summary COLLATE NOCASE LIKE '%' || :keyword || '%'
              OR ocrRefinedText COLLATE NOCASE LIKE '%' || :keyword || '%'
              OR tags COLLATE NOCASE LIKE '%' || :keyword || '%'
          )
        """,
    )
    fun getAnalyzedImageEntities(
        categoryId: Long?,
        favorite: Boolean?,
        keyword: String?,
    ): Flow<List<AnalyzedImageEntity>>

    @Query(
        """
        SELECT
            analyzed_images.*,
            categories.categoryId AS category_categoryId,
            categories.name AS category_name,
            categories.thumbnailUrl AS category_thumbnailUrl,
            categories.isNew AS category_isNew
        FROM analyzed_images
        INNER JOIN categories
            ON analyzed_images.categoryId = categories.categoryId
        WHERE analyzed_images.imageId = :imageId
        """,
    )
    fun getAnalyzedImageWithCategory(
        imageId: Long,
    ): Flow<AnalyzedImageWithCategory?>

    @Query(
        """
        SELECT *
        FROM analyzed_images
        WHERE imageId = :imageId
        """,
    )
    suspend fun getAnalyzedImageEntity(
        imageId: Long,
    ): AnalyzedImageEntity?

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    suspend fun insertCategoryIfAbsent(
        entity: CategoryEntity,
    )

    @Upsert
    suspend fun upsertAnalyzedImage(
        entity: AnalyzedImageEntity,
    )

    @Query(
        """
        UPDATE analyzed_images
        SET favorite = :favorite
        WHERE imageId = :imageId
        """,
    )
    suspend fun updateFavorite(
        imageId: Long,
        favorite: Boolean,
    )

    @Query(
        """
        UPDATE analyzed_images
        SET isCalendared = :isCalendared
        WHERE imageId = :imageId
        """,
    )
    suspend fun updateCalendared(
        imageId: Long,
        isCalendared: Boolean,
    )

    @Query(
        """
        DELETE FROM analyzed_images
        WHERE imageId = :imageId
        """,
    )
    suspend fun deleteAnalyzedImage(
        imageId: Long,
    )

    @Transaction
    suspend fun upsertAnalyzedImageWithCategory(
        categoryEntity: CategoryEntity,
        analyzedImageEntity: AnalyzedImageEntity,
    ) {
        insertCategoryIfAbsent(
            entity = categoryEntity,
        )

        upsertAnalyzedImage(
            entity = analyzedImageEntity,
        )
    }
}