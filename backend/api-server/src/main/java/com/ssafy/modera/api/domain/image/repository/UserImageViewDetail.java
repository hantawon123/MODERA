package com.ssafy.modera.api.domain.image.repository;

import java.time.OffsetDateTime;
import java.util.List;

public record UserImageViewDetail(
        String s3Key,
        String thumbnailKey,
        String uploadStatus,
        String analysisStatus,
        String title,
        Boolean favorite,
        String summary,
        Integer categoryId,
        String categoryName,
        List<String> tagNames,
        List<String> keyInformation,
        String structuredDataJson,
        String ocrRefinedText,
        OffsetDateTime uploadedAt,
        Boolean documented,
        Boolean calendared
) {
}
