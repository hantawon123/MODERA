package com.ssafy.modera.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.model.document.Document

@Entity(
    tableName = "documents",
)
data class DocumentEntity(
    @PrimaryKey
    val documentId: Long,
    val name: String,
    val summary: String,
    val content: String,
    val imageCount: Int,
    val deletedImageCount: Int,
    val regenerating: Boolean,
    val updatedAt: Long,
)

fun DocumentEntity.asDocument(): Document =
    Document(
        id = documentId,
        title = name,
        content = summary,
        sourceImageCount = imageCount,
        updatedAt = updatedAt,
    )

fun DocumentEntity.asDocumentDetail(
    imageIds: List<Long>,
): DocumentDetail =
    DocumentDetail(
        id = documentId,
        name = name,
        summary = summary,
        content = content,
        imageCount = imageCount,
        deletedImageCount = deletedImageCount,
        imageIds = imageIds,
        regenerating = regenerating,
        updatedAt = updatedAt,
    )