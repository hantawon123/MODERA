package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ImageFavoriteRequest(
        @Schema(description = "즐겨찾기 설정 여부", example = "true")
        @NotNull
        Boolean favorite
) {
}
