package com.ssafy.modera.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage

@Entity(
    tableName = "analyzed_images",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["updatedAt"]),
    ],
)
data class AnalyzedImageEntity(
    @PrimaryKey
    val imageId: Long,
    val categoryId: Long,
    val imageUrl: String?,
    val thumbnailUrl: String,
    val title: String,
    val summary: String,
    val favorite: Boolean,
    val tags: List<String>,
    val keyInformation: List<String>,
    val ocrRefinedText: String,
    val isDocumented: Boolean,
    val isCalendared: Boolean,
    val updatedAt: Long,
)

fun AnalyzedImageEntity.asExternalModel(): AnalyzedImage =
    AnalyzedImage(
        id = imageId,
        title = title,
        summary = summary,
        thumbnailUrl = thumbnailUrl,
        hashtags = tags,
        favorite = favorite,
        isDocumented = isDocumented,
        hasSchedule = isCalendared,
    )