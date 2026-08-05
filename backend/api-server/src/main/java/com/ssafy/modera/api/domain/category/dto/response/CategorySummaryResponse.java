package com.ssafy.modera.api.domain.category.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record CategorySummaryResponse(
        @Schema(description = "카테고리 ID") Integer categoryId,
        @Schema(description = "카테고리 이름") String name,
        @Schema(description = "AI가 생성한 카테고리 아이콘 presigned GET URL(1시간 유효). "
                + "아이콘 생성이 백그라운드라 카테고리 생성 직후에는 URL이 404일 수 있다")
        String categoryImageUrl,
        @Schema(description = "사용자의 활성 이미지 수") Integer imageCount,
        @Schema(description = "최신 이미지 업로드 시각") OffsetDateTime latestUpdatedAt
) {
}
