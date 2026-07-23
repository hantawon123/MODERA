package com.ssafy.modera.api.domain.query.repository;

import java.time.Instant;
import java.util.List;

/**
 * query_schema.user_image_view upsert 입력. 원본이 아니라 이벤트를 합친 사본이므로
 * JPA 엔티티가 아니라 쓰기 전용 row로만 다룬다.
 */
public record UserImageViewRow(
        Integer userId,
        Integer imageId,
        String nickname,
        String fileName,
        String s3Key,
        String thumbnailKey,
        String title,
        String summary,
        String categoryName,
        List<String> tagNames,
        String uploadStatus,
        String analysisStatus,
        Boolean favorite,
        Instant createdAt,
        Instant updatedAt
) {
}
