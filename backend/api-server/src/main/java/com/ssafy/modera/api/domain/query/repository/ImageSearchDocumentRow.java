package com.ssafy.modera.api.domain.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageSearchDocumentRow(
        UUID imageId,
        Long userId,
        String title,
        String summary,
        String ocrText,
        String categoryName,
        List<String> tagNames,
        String structuredFieldsJson,
        Instant indexedAt
) {
}
