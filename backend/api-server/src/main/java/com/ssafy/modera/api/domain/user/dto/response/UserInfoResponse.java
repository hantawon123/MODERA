package com.ssafy.modera.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserInfoResponse(
        @Schema(example = "1") Integer userId,
        @Schema(example = "newUser123", nullable = true) String loginId,
        @Schema(example = "user@example.com", nullable = true) String email,
        @Schema(example = "true") boolean notification,
        @Schema(example = "true") boolean backgroundAnalysis
) {
}
