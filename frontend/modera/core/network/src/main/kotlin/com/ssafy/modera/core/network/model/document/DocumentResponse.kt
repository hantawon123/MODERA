package com.ssafy.modera.core.network.model.document

import com.ssafy.modera.core.model.document.Document
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class DocumentResponse(
    val documentId: Long,
    val name: String,
    val summary: String,
    val imageCount: Int,
    val delImageCount: Int,
    val updatedAt: String,
)

fun DocumentResponse.asExternalModel(): Document =
    Document(
        id = documentId,
        title = name,
        content = summary,
        sourceImageCount = imageCount,
        updatedAt = Instant
            .parse(updatedAt)
            .toEpochMilli(),
    )