package com.ssafy.modera.api.domain.query.repository;

import java.util.List;

/**
 * query_schema.user_image_view의 목록용 조회 결과. 5-1 목록 응답 아이템에 필요한
 * 필드만 담는다(썸네일은 key까지만 — presigned URL 변환은 서비스 계층 책임).
 * <p>
 * tags 컬럼은 {@code [{"tagId":..,"name":..}]} JSONB지만 화면은 이름 배열만 쓰므로
 * SQL에서 이름만 뽑아 순서대로 담는다.
 */
public record UserImageViewSummary(
        Integer imageId,
        String title,
        String summary,
        Boolean favorite,
        String thumbnailKey,
        List<String> tagNames,
        String categoryName
) {
}
