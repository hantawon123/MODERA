package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.database.model.DocumentEntity
import com.ssafy.modera.core.network.model.document.DocumentDetailResponse
import java.time.Instant

internal fun DocumentDetailResponse.asEntity(): DocumentEntity =
    DocumentEntity(
        documentId = documentId,
        name = name,
        summary = summary,
        content = content,
        imageCount = imageCount,
        deletedImageCount = delImageCount,
        regenerating = regenerating,
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
    )