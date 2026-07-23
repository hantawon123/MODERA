package com.ssafy.modera.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterResponse(
        @Schema(description = "새로 발급된 사용자 ID", example = "1") Long userId
) {
}
