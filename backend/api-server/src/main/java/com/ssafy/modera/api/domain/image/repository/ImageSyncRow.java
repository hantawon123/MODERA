package com.ssafy.modera.api.domain.image.repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 5-10 전체 동기화(로컬 DB 복원)용 행. 상세(5-2)와 같은 필드 구성에 imageId가 붙고,
 * presigned URL 재료(s3_key 등)는 없다 — 동기화 응답에 URL을 싣지 않기 때문이다.
 */
public record ImageSyncRow(
        Integer imageId,
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
