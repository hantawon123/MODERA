package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageFavoriteResponse(
        @Schema(description = "이미지 ID", example = "1024")
        Integer imageId,

        @Schema(description = "변경된 즐겨찾기 여부", example = "true")
        boolean favorite,

        @Schema(description = "변경 직후 활성 즐겨찾기 이미지 개수", example = "120")
        int favoriteCount
) {
}
