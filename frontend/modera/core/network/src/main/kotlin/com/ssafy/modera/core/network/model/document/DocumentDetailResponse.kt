package com.ssafy.modera.core.network.model.document

import com.ssafy.modera.core.model.DocumentDetail
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class DocumentDetailResponse(
    val documentId: Long,
    val name: String,
    val summary: String,
    val content: String,
    val imageCount: Int,
    val delImageCount: Int,
    val imageIds: List<Long>,
    val regenerating: Boolean,
    val updatedAt: String,
)

fun DocumentDetailResponse.asExternalModel(): DocumentDetail =
    DocumentDetail(
        id = documentId,
        name = name,
        summary = summary,
        content = content,
        imageCount = imageCount,
        deletedImageCount = delImageCount,
        imageIds = imageIds,
        regenerating = regenerating,
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
    )