package com.ssafy.modera.api.domain.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record CategorySummaryResponse(
        @Schema(description = "카테고리 ID") Integer categoryId,
        @Schema(description = "카테고리 이름") String name,
        @Schema(description = "카테고리 이미지 presigned GET URL") String categoryImageUrl,
        @Schema(description = "사용자의 활성 이미지 수") Integer imageCount,
        @Schema(description = "최신 이미지 업로드 시각") OffsetDateTime latestUpdatedAt
) {
}
