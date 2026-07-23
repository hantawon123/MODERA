package com.ssafy.modera.api.domain.query.repository;

import java.time.Instant;
import java.util.List;

public record ImageSearchDocumentRow(
        Integer imageId,
        Integer userId,
        String title,
        String summary,
        String ocrText,
        String categoryName,
        List<String> tagNames,
        String structuredFieldsJson,
        Instant indexedAt
) {
}
