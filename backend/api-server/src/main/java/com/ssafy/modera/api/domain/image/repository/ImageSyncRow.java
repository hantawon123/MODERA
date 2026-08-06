package com.ssafy.modera.api.domain.image.repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 5-10 전체 동기화(로컬 DB 복원)용 행. 상세(5-2)와 같은 필드 구성에 imageId와
 * presigned URL 재료(s3_key·thumbnail_key)가 붙는다 — URL은 서비스가 행마다 서명한다.
 */
public record ImageSyncRow(
        Integer imageId,
        String s3Key,
        String thumbnailKey,
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
